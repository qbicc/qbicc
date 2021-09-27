package org.qbicc.plugin.serialization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmArrayClass;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.object.Data;
import org.qbicc.object.Linkage;
import org.qbicc.object.Section;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.ArrayType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.Primitive;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;

public class BuildtimeHeap {
    private static final AttachmentKey<BuildtimeHeap> KEY = new AttachmentKey<>();
    private static final String prefix = "qbicc_initial_heap_obj_";

    private final CompilationContext ctxt;
    private final Layout layout;
    private final CoreClasses coreClasses;
    /**
     * For lazy definition of native array types for literals
     */
    private final HashMap<String, CompoundType> arrayTypes = new HashMap<>();
    /**
     * For interning java.lang.Class instances
     */
    private final HashMap<LoadedTypeDefinition, Data> classObjects = new HashMap<>();
    /**
     * For interning VmObjects
     */
    private final IdentityHashMap<VmObject, Data> vmObjects = new IdentityHashMap<>();
    /**
     * The initial heap
     */
    private final Section heapSection;
    /**
     * The global array of java.lang.Class instances that is part of the serialized heap
     */
    private GlobalVariableElement classArrayGlobal;

    private int literalCounter = 0;

    private BuildtimeHeap(CompilationContext ctxt) {
        this.ctxt = ctxt;
        this.layout = Layout.get(ctxt);
        this.coreClasses = CoreClasses.get(ctxt);

        LoadedTypeDefinition ih = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/InitialHeap").load();
        this.heapSection = ctxt.getOrAddProgramModule(ih).getOrAddSection(ctxt.IMPLICIT_SECTION_NAME); // TODO: use ctxt.INITIAL_HEAP_SECTION_NAME
    }

    public static BuildtimeHeap get(CompilationContext ctxt) {
        BuildtimeHeap heap = ctxt.getAttachment(KEY);
        if (heap == null) {
            heap = new BuildtimeHeap(ctxt);
            BuildtimeHeap appearing = ctxt.putAttachmentIfAbsent(KEY, heap);
            if (appearing != null) {
                heap = appearing;
            }
        }
        return heap;
    }

    void setClassArrayGlobal(GlobalVariableElement g) {
        this.classArrayGlobal = g;
    }

    public GlobalVariableElement getAndRegisterGlobalClassArray(ExecutableElement originalElement) {
        Assert.assertNotNull(classArrayGlobal);
        if (!classArrayGlobal.getEnclosingType().equals(originalElement.getEnclosingType())) {
            Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
            section.declareData(null, classArrayGlobal.getName(), classArrayGlobal.getType());
        }
        return classArrayGlobal;
    }

    public synchronized Data serializeVmObject(VmObject value) {
        if (vmObjects.containsKey(value)) {
            return vmObjects.get(value);
        }
        PhysicalObjectType ot = value.getObjectType();
        Data sl;
        if (ot instanceof ClassObjectType) {
            if (value instanceof VmClass) {
                if (value instanceof VmArrayClass) {
                    if (((VmArrayClass)value).getInstanceObjectType().getElementType() instanceof ReferenceArrayObjectType) {
                        ctxt.error("Serialization of reference array class objects not supported: "+((VmArrayClass)value).getInstanceObjectType());
                    }
                }
                // Redirect to the class objects already serialized by the ANALYZE post hook ClassObjectSerializer.
                sl = classObjects.get(((VmClass)value).getTypeDefinition());
                if (sl == null) {
                    ctxt.error("Serializing java.lang.Class instance for unreachable class: "+((VmClass)value).getTypeDefinition());
                }
            } else {
                sl = serializeVmObject((ClassObjectType) ot, value);
            }
        } else if (ot instanceof ReferenceArrayObjectType) {
            sl = serializeRefArray((ReferenceArrayObjectType) ot, (VmArray)value);
        } else {
            sl = serializePrimArray((PrimitiveArrayObjectType) ot, (VmArray)value);
        }
        vmObjects.put(value, sl);
        return sl;
    }

    public synchronized Data serializeClassObject(Primitive primitive) {
        LoadedTypeDefinition jlc = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load();
        Literal classLiteral = createClassObjectLiteral(jlc, primitive.getName(), primitive.getType());
        return defineData(nextLiteralName(), classLiteral);
    }

    public synchronized Data serializeClassObject(LoadedTypeDefinition type) {
        if (classObjects.containsKey(type)) {
            return classObjects.get(type);
        }

        LoadedTypeDefinition jlc = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load();
        Data cl = serializeClassObjectImpl(jlc, type);
        classObjects.put(type, cl);
        return cl;
    }

    private Literal createClassObjectLiteral(LoadedTypeDefinition jlc, String className, ValueType type) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        Layout.LayoutInfo jlcLayout = layout.getInstanceLayoutInfo(jlc);
        CompoundType jlcType = jlcLayout.getCompoundType();
        HashMap<CompoundType.Member, Literal> memberMap = new HashMap<>();

        // First default to a zero initializer for all the instance fields
        for (CompoundType.Member m : jlcType.getMembers()) {
            memberMap.put(m, lf.zeroInitializerLiteralOfType(m.getType()));
        }

        // Object header
        memberMap.put(jlcLayout.getMember(coreClasses.getObjectTypeIdField()), lf.literalOf(jlc.getTypeId()));

        // Next, initialize instance fields of the qbicc jlc type that we can/want to set at build time.
        CompoundType.Member name = jlcType.getMember("name");
        memberMap.put(name, dataToLiteral(lf, serializeVmObject(ctxt.getVm().intern(className)), (WordType) name.getType()));

        CompoundType.Member id = jlcType.getMember("id");
        memberMap.put(id, lf.literalOfType(type));

        //
        // TODO: Figure out what information we need to support reflection and serialize it too (VMClass instance???)
        //

        return ctxt.getLiteralFactory().literalOf(jlcType, memberMap);
    }

    private Data serializeClassObjectImpl(LoadedTypeDefinition jlc, LoadedTypeDefinition type) {
        String externalName = type.getInternalName().replace('/', '.');
        if (externalName.startsWith("internal_array_")) {
            externalName = externalName.replaceFirst("internal_array_", "[");
        }
        Literal classLiteral = createClassObjectLiteral(jlc, externalName, type.getType());
        return defineData(nextLiteralName(), classLiteral);
    }

    private String nextLiteralName() {
        return prefix + (this.literalCounter++);
    }

    private Data defineData(String name, Literal value) {
        Data d = heapSection.addData(null, name, value);
        d.setLinkage(Linkage.EXTERNAL);
        d.setAddrspace(1);
        return d;
    }

    private Literal dataToLiteral(LiteralFactory lf, Data data, WordType toType) {
        return lf.bitcastLiteral(lf.literalOfSymbol(data.getName(), data.getType().getPointer().asCollected()), toType);
    }

    private CompoundType arrayLiteralType(FieldElement contents, int length) {
        LoadedTypeDefinition ltd = contents.getEnclosingType().load();
        String typeName = ltd.getInternalName() + "_" + length;
        CompoundType sizedArrayType = arrayTypes.get(typeName);
        if (sizedArrayType == null) {
            TypeSystem ts = ctxt.getTypeSystem();
            Layout.LayoutInfo objLayout = layout.getInstanceLayoutInfo(ltd);
            CompoundType arrayCT = objLayout.getCompoundType();

            CompoundType.Member contentMem = objLayout.getMember(contents);
            ArrayType sizedContentMem = ts.getArrayType(((ArrayType) contents.getType()).getElementType(), length);
            CompoundType.Member realContentMem = ts.getCompoundTypeMember(contentMem.getName(), sizedContentMem, contentMem.getOffset(), contentMem.getAlign());

            Supplier<List<CompoundType.Member>> thunk;
            if (contents.equals(coreClasses.getRefArrayContentField())) {
                thunk = () -> List.of(arrayCT.getMember(0), arrayCT.getMember(1), arrayCT.getMember(2), arrayCT.getMember(3), realContentMem);
            } else {
                thunk = () -> List.of(arrayCT.getMember(0), arrayCT.getMember(1), realContentMem);
            }

            sizedArrayType = ts.getCompoundType(CompoundType.Tag.STRUCT, typeName, arrayCT.getSize() + sizedContentMem.getSize(), arrayCT.getAlign(), thunk);
            arrayTypes.put(typeName, sizedArrayType);
        }
        return sizedArrayType;
    }

    private Data serializeVmObject(ClassObjectType ct, VmObject value) {
        Memory memory = value.getMemory();
        LiteralFactory lf = ctxt.getLiteralFactory();
        LoadedTypeDefinition concreteType = ct.getDefinition().load();
        Layout.LayoutInfo objLayout = layout.getInstanceLayoutInfo(concreteType);
        CompoundType objType = objLayout.getCompoundType();
        HashMap<CompoundType.Member, Literal> memberMap = new HashMap<>();

        // Iterate over instance fields and get their values from value's backing Memory
        CompoundType.Member typeIdMember = objLayout.getMember(coreClasses.getObjectTypeIdField());
        for (CompoundType.Member member : objType.getMembers()) {
            if (member.equals(typeIdMember)) {
                // Handle specially; not set by the interpreter because typeIds are assigned in postHook of Phase.ANALYZE.
                memberMap.put(typeIdMember, lf.literalOf(concreteType.getTypeId()));
            } else if (member.getType() instanceof IntegerType) {
                IntegerType it = (IntegerType)member.getType();
                if (it.getSize() == 8L) {
                    memberMap.put(member, lf.literalOf(it, memory.load8(member.getOffset(), MemoryAtomicityMode.UNORDERED)));
                } else if (it.getSize() == 16L) {
                    memberMap.put(member, lf.literalOf(it, memory.load16(member.getOffset(), MemoryAtomicityMode.UNORDERED)));
                } else if (it.getSize() == 32L) {
                    memberMap.put(member, lf.literalOf(it, memory.load32(member.getOffset(), MemoryAtomicityMode.UNORDERED)));
                } else {
                    memberMap.put(member, lf.literalOf(it, memory.load64(member.getOffset(), MemoryAtomicityMode.UNORDERED)));
                }
            } else if (member.getType() instanceof FloatType) {
                FloatType ft = (FloatType)member.getType();
                if (ft.getSize() == 32L) {
                    memberMap.put(member, lf.literalOf(ft, memory.loadFloat(member.getOffset(), MemoryAtomicityMode.UNORDERED)));
                } else {
                    memberMap.put(member, lf.literalOf(ft, memory.loadDouble(member.getOffset(), MemoryAtomicityMode.UNORDERED)));
                }
            } else {
                VmObject contents = memory.loadRef(member.getOffset(), MemoryAtomicityMode.UNORDERED);
                if (contents == null) {
                    memberMap.put(member, lf.zeroInitializerLiteralOfType(member.getType()));
                } else {
                    memberMap.put(member, dataToLiteral(lf, serializeVmObject(contents), (WordType) member.getType()));
                }
            }
        }

        return defineData(nextLiteralName(), ctxt.getLiteralFactory().literalOf(objType, memberMap));
    }

    private Data serializeRefArray(ReferenceArrayObjectType at, VmArray value) {
        LoadedTypeDefinition jlo = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load();
        LiteralFactory lf = ctxt.getLiteralFactory();
        FieldElement contentsField = coreClasses.getRefArrayContentField();
        Layout.LayoutInfo info = layout.getInstanceLayoutInfo(contentsField.getEnclosingType());

        Memory memory = value.getMemory();
        int length = memory.load32(info.getMember(coreClasses.getArrayLengthField()).getOffset(), MemoryAtomicityMode.UNORDERED);
        CompoundType literalCT = arrayLiteralType(contentsField, length);

        List<Literal> elements = new ArrayList<>(length);
        for (int i=0; i<length; i++) {
            VmObject e = memory.loadRef(value.getArrayElementOffset(i), MemoryAtomicityMode.UNORDERED);
            if (e == null) {
                elements.add(lf.zeroInitializerLiteralOfType(at.getElementType()));
            } else {
                Data elem = serializeVmObject(e);
                elements.add(lf.bitcastLiteral(lf.literalOfSymbol(elem.getName(), elem.getType().getPointer().asCollected()), jlo.getClassType().getReference()));
            }
        }

        Literal al = lf.literalOf(literalCT, Map.of(
            literalCT.getMember(0), lf.literalOf(contentsField.getEnclosingType().load().getTypeId()),
            literalCT.getMember(1), lf.literalOf(length),
            literalCT.getMember(2), lf.literalOf(ctxt.getTypeSystem().getUnsignedInteger8Type(), at.getDimensionCount()),
            literalCT.getMember(3), lf.literalOf(at.getLeafElementType().getDefinition().load().getTypeId()),
            literalCT.getMember(4), ctxt.getLiteralFactory().literalOf(ctxt.getTypeSystem().getArrayType(jlo.getClassType().getReference(), length), elements)
        ));

        return defineData(nextLiteralName(), al);
    }

    private Data serializePrimArray(PrimitiveArrayObjectType at, VmArray value) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        TypeSystem ts = ctxt.getTypeSystem();
        FieldElement contentsField = coreClasses.getArrayContentField(at);
        Layout.LayoutInfo info = layout.getInstanceLayoutInfo(contentsField.getEnclosingType());

        Memory memory = value.getMemory();
        int length = memory.load32(info.getMember(coreClasses.getArrayLengthField()).getOffset(), MemoryAtomicityMode.UNORDERED);
        CompoundType literalCT = arrayLiteralType(contentsField, length);

        Literal arrayContentsLiteral;
        if (contentsField.equals(coreClasses.getByteArrayContentField())) {
            byte[] contents = new byte[length];
            for (int i=0; i<length; i++) {
                contents[i] = (byte)memory.load8(value.getArrayElementOffset(i), MemoryAtomicityMode.UNORDERED);
            }
            arrayContentsLiteral = lf.literalOf(ctxt.getTypeSystem().getArrayType(at.getElementType(), length), contents);
        } else {
            List<Literal> elements = new ArrayList<>(length);
            if (contentsField.equals(coreClasses.getBooleanArrayContentField())) {
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(memory.load8(value.getArrayElementOffset(i), MemoryAtomicityMode.UNORDERED) > 0));
                }
            } else if (contentsField.equals(coreClasses.getShortArrayContentField())) {
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(ts.getSignedInteger16Type(), memory.load16(value.getArrayElementOffset(i), MemoryAtomicityMode.UNORDERED)));
                }
            } else if (contentsField.equals(coreClasses.getCharArrayContentField())) {
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(ts.getUnsignedInteger16Type(), memory.load16(value.getArrayElementOffset(i), MemoryAtomicityMode.UNORDERED)));
                }
            } else if (contentsField.equals(coreClasses.getIntArrayContentField())) {
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(ts.getSignedInteger32Type(), memory.load32(value.getArrayElementOffset(i), MemoryAtomicityMode.UNORDERED)));
                }
            } else if (contentsField.equals(coreClasses.getLongArrayContentField())) {
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(ts.getSignedInteger64Type(), memory.load64(value.getArrayElementOffset(i), MemoryAtomicityMode.UNORDERED)));
                }
            } else if (contentsField.equals(coreClasses.getFloatArrayContentField())) {
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(ts.getFloat32Type(), memory.loadFloat(value.getArrayElementOffset(i), MemoryAtomicityMode.UNORDERED)));
                }
            } else {
                Assert.assertTrue((contentsField.equals(coreClasses.getDoubleArrayContentField())));
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(ts.getFloat64Type(), memory.loadDouble(value.getArrayElementOffset(i), MemoryAtomicityMode.UNORDERED)));
                }
            }
            arrayContentsLiteral = lf.literalOf(ctxt.getTypeSystem().getArrayType(at.getElementType(), length), elements);
        }

        Literal arrayLiteral = lf.literalOf(literalCT, Map.of(
            literalCT.getMember(0), lf.literalOf(contentsField.getEnclosingType().load().getTypeId()),
            literalCT.getMember(1), lf.literalOf(length),
            literalCT.getMember(2), arrayContentsLiteral
        ));

        return defineData(nextLiteralName(), arrayLiteral);
    }
}

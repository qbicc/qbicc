package org.qbicc.plugin.serialization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Supplier;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.object.Data;
import org.qbicc.object.Linkage;
import org.qbicc.object.Section;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.ArrayType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.Primitive;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;

public class BuildtimeHeap {
    private static final AttachmentKey<BuildtimeHeap> KEY = new AttachmentKey<>();
    private static final String prefix = "qbicc_initial_heap_obj_";

    private final CompilationContext ctxt;
    private final Layout interpreterLayout;
    private final CoreClasses coreClasses;
    /**
     * For lazy definition of native array types for literals
     */
    private final HashMap<String, CompoundType> arrayTypes = new HashMap<>();
    /**
     * For interning java.lang.Class instances
     */
    private final HashMap<LoadedTypeDefinition, SymbolLiteral> classObjects = new HashMap<>();
    /**
     * For interning java.lang.Class instances that correspond to Primitives
     */
    private final HashMap<String, SymbolLiteral> primitiveClassObjects = new HashMap<>();
    /**
     * For interning VmObjects
     */
    private final IdentityHashMap<VmObject, SymbolLiteral> vmObjects = new IdentityHashMap<>();
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
        this.interpreterLayout = Layout.getForInterpreter(ctxt);
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

    public SymbolLiteral getSerializedVmObject(VmObject value) {
        return vmObjects.get(value);
    }

    public synchronized SymbolLiteral serializeVmObject(VmObject value) {
        if (vmObjects.containsKey(value)) {
            return vmObjects.get(value);
        }
        Layout layout = Layout.get(ctxt);
        PhysicalObjectType ot = value.getObjectType();
        SymbolLiteral sl;
        if (ot instanceof ClassObjectType) {
            // Could be part of a cyclic object graph; must record the symbol for this object before we serialize its fields
            LoadedTypeDefinition concreteType = ot.getDefinition().load();
            LayoutInfo objLayout = layout.getInstanceLayoutInfo(concreteType);
            sl = ctxt.getLiteralFactory().literalOfSymbol(nextLiteralName(), objLayout.getCompoundType());
            vmObjects.put(value, sl);

            serializeVmObject(concreteType, objLayout, sl, value);
        } else if (ot instanceof ReferenceArrayObjectType) {
            // Could be part of a cyclic object graph; must record the symbol for this array before we serialize its elements
            FieldElement contentsField = coreClasses.getRefArrayContentField();
            LayoutInfo info = layout.getInstanceLayoutInfo(contentsField.getEnclosingType());
            Memory memory = value.getMemory();
            int length = memory.load32(info.getMember(coreClasses.getArrayLengthField()).getOffset(), MemoryAtomicityMode.UNORDERED);
            CompoundType literalCT = arrayLiteralType(contentsField, length);
            sl = ctxt.getLiteralFactory().literalOfSymbol(nextLiteralName(), literalCT);
            vmObjects.put(value, sl);

            serializeRefArray((ReferenceArrayObjectType) ot, literalCT, length, sl, (VmArray)value);
        } else {
            // Can't be cyclic; ok to serialize then record the ref.
            sl = serializePrimArray((PrimitiveArrayObjectType) ot, (VmArray)value);
            vmObjects.put(value, sl);
        }
        vmObjects.put(value, sl);
        return sl;
    }

    public synchronized SymbolLiteral serializeClassObject(Primitive primitive) {
        if (primitiveClassObjects.containsKey(primitive.getName())) {
            return primitiveClassObjects.get(primitive.getName());
        }
        SymbolLiteral sl = serializeVmObject(ctxt.getVm().getPrimitiveClass(primitive));
        if (sl != null) {
            primitiveClassObjects.put(primitive.getName(), sl);
        }
        return sl;
    }

    public synchronized SymbolLiteral serializeClassObject(LoadedTypeDefinition type) {
        if (classObjects.containsKey(type)) {
            return classObjects.get(type);
        }
        VmClass vmClass = type.load().getVmClass();
        SymbolLiteral sl = serializeVmObject(vmClass);
        if (sl != null) {
            classObjects.put(type, sl);
        }
        return sl;
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

    private Literal castLiteral(LiteralFactory lf, SymbolLiteral sl, WordType toType) {
        return lf.bitcastLiteral(lf.literalOfSymbol(sl.getName(), sl.getType().getPointer().asCollected()), toType);
    }

    private CompoundType arrayLiteralType(FieldElement contents, int length) {
        LoadedTypeDefinition ltd = contents.getEnclosingType().load();
        String typeName = ltd.getInternalName() + "_" + length;
        CompoundType sizedArrayType = arrayTypes.get(typeName);
        Layout layout = Layout.get(ctxt);
        if (sizedArrayType == null) {
            TypeSystem ts = ctxt.getTypeSystem();
            LayoutInfo objLayout = layout.getInstanceLayoutInfo(ltd);
            CompoundType arrayCT = objLayout.getCompoundType();

            CompoundType.Member contentMem = objLayout.getMember(contents);
            ArrayType sizedContentMem = ts.getArrayType(((ArrayType) contents.getType()).getElementType(), length);
            CompoundType.Member realContentMem = ts.getCompoundTypeMember(contentMem.getName(), sizedContentMem, contentMem.getOffset(), contentMem.getAlign());

            Supplier<List<CompoundType.Member>> thunk = () -> {
                CompoundType.Member[] items = arrayCT.getMembers().toArray(CompoundType.Member[]::new);
                for (int i = 0; i < items.length; i++) {
                    if (items[i] == contentMem) {
                        items[i] = realContentMem;
                    }
                }
                return Arrays.asList(items);
            };

            sizedArrayType = ts.getCompoundType(CompoundType.Tag.STRUCT, typeName, arrayCT.getSize() + sizedContentMem.getSize(), arrayCT.getAlign(), thunk);
            arrayTypes.put(typeName, sizedArrayType);
        }
        return sizedArrayType;
    }

    private void serializeVmObject(LoadedTypeDefinition concreteType, LayoutInfo objLayout, SymbolLiteral sl, VmObject value) {
        Memory memory = value.getMemory();
        LayoutInfo memLayout = interpreterLayout.getInstanceLayoutInfo(concreteType);
        CompoundType objType = objLayout.getCompoundType();
        HashMap<CompoundType.Member, Literal> memberMap = new HashMap<>();

        populateMemberMap(concreteType, objType, objLayout, memLayout, memory, memberMap);

        // Define it!
        defineData(sl.getName(), ctxt.getLiteralFactory().literalOf(objType, memberMap));
    }

    private void populateMemberMap(final LoadedTypeDefinition concreteType, final CompoundType objType, final LayoutInfo objLayout, final LayoutInfo memLayout, final Memory memory, final HashMap<CompoundType.Member, Literal> memberMap) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        // Start by zero-initializing all members
        for (CompoundType.Member m : objType.getMembers()) {
            memberMap.put(m, lf.zeroInitializerLiteralOfType(m.getType()));
        }

        populateClearedMemberMap(concreteType, objType, objLayout, memLayout, memory, memberMap);
    }

    private void populateClearedMemberMap(final LoadedTypeDefinition concreteType, final CompoundType objType, final LayoutInfo objLayout, final LayoutInfo memLayout, final Memory memory, final HashMap<CompoundType.Member, Literal> memberMap) {
        if (concreteType.hasSuperClass()) {
            populateClearedMemberMap(concreteType.getSuperClass(), objType, objLayout, memLayout, memory, memberMap);
        }

        LiteralFactory lf = ctxt.getLiteralFactory();
        // Next, iterate over object's instance fields and copy values from the backing Memory to the memberMap
        int fc = concreteType.getFieldCount();
        for (int i=0; i<fc; i++) {
            FieldElement f = concreteType.getField(i);
            if (f.isStatic()) {
                continue;
            }
            CompoundType.Member im = memLayout.getMember(f);
            CompoundType.Member om = objLayout.getMember(f);
            if (im.getType() instanceof IntegerType) {
                IntegerType it = (IntegerType)im.getType();
                if (it.getSize() == 1) {
                    memberMap.put(om, lf.literalOf(it, memory.load8(im.getOffset(), MemoryAtomicityMode.UNORDERED)));
                } else if (it.getSize() == 2) {
                    memberMap.put(om, lf.literalOf(it, memory.load16(im.getOffset(), MemoryAtomicityMode.UNORDERED)));
                } else if (it.getSize() == 4) {
                    memberMap.put(om, lf.literalOf(it, memory.load32(im.getOffset(), MemoryAtomicityMode.UNORDERED)));
                } else {
                    memberMap.put(om, lf.literalOf(it, memory.load64(im.getOffset(), MemoryAtomicityMode.UNORDERED)));
                }
            } else if (im.getType() instanceof FloatType) {
                FloatType ft = (FloatType)im.getType();
                if (ft.getSize() == 4) {
                    memberMap.put(om, lf.literalOf(ft, memory.loadFloat(im.getOffset(), MemoryAtomicityMode.UNORDERED)));
                } else {
                    memberMap.put(om, lf.literalOf(ft, memory.loadDouble(im.getOffset(), MemoryAtomicityMode.UNORDERED)));
                }
            } else if (im.getType() instanceof TypeType) {
                ValueType type = memory.loadType(im.getOffset(), MemoryAtomicityMode.UNORDERED);
                memberMap.put(om, type == null ? lf.zeroInitializerLiteralOfType(im.getType()) : lf.literalOfType(type));
            } else if (im.getType() instanceof ArrayType) {
                if (im.getType().getSize() > 0) {
                    throw new UnsupportedOperationException("Copying array data is not yet supported");
                }
            } else if (im.getType() instanceof ReferenceType) {
                VmObject contents = memory.loadRef(im.getOffset(), MemoryAtomicityMode.UNORDERED);
                if (contents == null) {
                    memberMap.put(om, lf.zeroInitializerLiteralOfType(om.getType()));
                } else {
                    memberMap.put(om, castLiteral(lf, serializeVmObject(contents), (WordType) om.getType()));
                }
            } else {
                throw new UnsupportedOperationException("Serialization of unsupported member type: " + im.getType());
            }
        }
    }

    private void serializeRefArray(ReferenceArrayObjectType at, CompoundType literalCT, int length, SymbolLiteral sl, VmArray value) {
        LoadedTypeDefinition jlo = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load();
        LiteralFactory lf = ctxt.getLiteralFactory();

        Layout layout = Layout.get(ctxt);
        Memory memory = value.getMemory();
        FieldElement contentField = coreClasses.getRefArrayContentField();
        DefinedTypeDefinition concreteType = contentField.getEnclosingType();
        LayoutInfo objLayout = layout.getInstanceLayoutInfo(concreteType);
        LayoutInfo memLayout = interpreterLayout.getInstanceLayoutInfo(concreteType);
        CompoundType objType = objLayout.getCompoundType();
        HashMap<CompoundType.Member, Literal> memberMap = new HashMap<>();

        populateMemberMap(concreteType.load(), objType, objLayout, memLayout, memory, memberMap);

        List<Literal> elements = new ArrayList<>(length);
        for (int i=0; i<length; i++) {
            VmObject e = memory.loadRef(value.getArrayElementOffset(i), MemoryAtomicityMode.UNORDERED);
            if (e == null) {
                elements.add(lf.zeroInitializerLiteralOfType(at.getElementType()));
            } else {
                SymbolLiteral elem = serializeVmObject(e);
                elements.add(lf.bitcastLiteral(lf.literalOfSymbol(elem.getName(), elem.getType().getPointer().asCollected()), jlo.getClassType().getReference()));
            }
        }

        ArrayType arrayType = ctxt.getTypeSystem().getArrayType(jlo.getClassType().getReference(), length);

        // add the actual array contents
        memberMap.put(literalCT.getMember(literalCT.getMemberCount() - 1), lf.literalOf(arrayType, elements));

        // Define it with the literal type we generated above
        defineData(sl.getName(), ctxt.getLiteralFactory().literalOf(literalCT, memberMap));
    }

    private SymbolLiteral serializePrimArray(PrimitiveArrayObjectType at, VmArray value) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        TypeSystem ts = ctxt.getTypeSystem();
        Layout layout = Layout.get(ctxt);
        FieldElement contentsField = coreClasses.getArrayContentField(at);
        DefinedTypeDefinition concreteType = contentsField.getEnclosingType();
        LayoutInfo objLayout = layout.getInstanceLayoutInfo(concreteType);
        LayoutInfo memLayout = interpreterLayout.getInstanceLayoutInfo(concreteType);
        CompoundType objType = objLayout.getCompoundType();

        Memory memory = value.getMemory();
        int length = memory.load32(objLayout.getMember(coreClasses.getArrayLengthField()).getOffset(), MemoryAtomicityMode.UNORDERED);
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
                    elements.add(lf.literalOf(memory.load8(value.getArrayElementOffset(i), MemoryAtomicityMode.UNORDERED) != 0));
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

        HashMap<CompoundType.Member, Literal> memberMap = new HashMap<>();

        populateMemberMap(concreteType.load(), objType, objLayout, memLayout, memory, memberMap);

        // add the actual array contents
        memberMap.put(literalCT.getMember(literalCT.getMemberCount() - 1), arrayContentsLiteral);

        Data arrayData = defineData(nextLiteralName(), ctxt.getLiteralFactory().literalOf(literalCT, memberMap));
        return lf.literalOfSymbol(arrayData.getName(), literalCT);
    }
}

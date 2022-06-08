package org.qbicc.plugin.serialization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Supplier;

import io.smallrye.common.constraint.Assert;
import org.jboss.logging.Logger;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmReferenceArrayClass;
import org.qbicc.object.Data;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.Linkage;
import org.qbicc.object.ModuleSection;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.ProgramObject;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.pointer.Pointer;
import org.qbicc.pointer.ProgramObjectPointer;
import org.qbicc.pointer.StaticMethodPointer;
import org.qbicc.type.ArrayType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.NullableType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;
import static org.qbicc.machine.arch.AddressSpaceConstants.COLLECTED;

public class BuildtimeHeap {
    private static final AttachmentKey<BuildtimeHeap> KEY = new AttachmentKey<>();
    private static final Logger slog = Logger.getLogger("org.qbicc.plugin.serialization.stats");

    private final CompilationContext ctxt;
    private final Layout layout;
    private final CoreClasses coreClasses;
    /**
     * For lazy definition of native array types for literals
     */
    private final HashMap<String, CompoundType> arrayTypes = new HashMap<>();
    /**
     * For interning VmObjects
     */
    private final IdentityHashMap<VmObject, DataDeclaration> vmObjects = new IdentityHashMap<>();
    /**
     * The array of root classes which is intended to be the first object in the initial heap
     */
    private final ModuleSection classSection;
    /**
     * Objects associated with build-time interned Strings.
     * These objects can be ignored by GC because (a) they are non-collectable and
     * (b) are guaranteed to not contain pointers to objects outside of this section.
     * This section includes:
     *   (a) The root array of all build-time interned Strings
     *   (b) The java.lang.String instances for all build-time interned Strings
     *   (c) The backing byte[] for all build-time interned Strings
     */
    private final ModuleSection stringSection;
    /**
     * The rest of the objects in the initial heap
     */
    private final ModuleSection objectSection;
    /**
     * The global array of root java.lang.Class instances
     */
    private DataDeclaration rootClassesDecl;
    /**
     * The values, indexed by typeId to be serialized in the classArrayGlobal
     */
    private Literal[] rootClasses;

    private int literalCounter = 0;

    private BuildtimeHeap(CompilationContext ctxt) {
        this.ctxt = ctxt;
        this.layout = Layout.get(ctxt);
        this.coreClasses = CoreClasses.get(ctxt);

        this.classSection = ctxt.getImplicitSection(ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/InitialHeap$ClassSection").load());
        this.stringSection = ctxt.getImplicitSection(ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/InitialHeap$InternedStringSection").load());
        this.objectSection = ctxt.getImplicitSection(ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/InitialHeap$ObjectSection").load());
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

    public static void reportStats(CompilationContext ctxt) {
        if (!slog.isDebugEnabled()) return;
        BuildtimeHeap heap = ctxt.getAttachment(KEY);
        slog.debugf("The initial heap contains %,d objects.", heap.vmObjects.size());
        HashMap<LoadedTypeDefinition, Integer> instanceCounts = new HashMap<>();
        for (VmObject obj : heap.vmObjects.keySet()) {
            LoadedTypeDefinition ltd = obj.getVmClass().getTypeDefinition();
            instanceCounts.put(ltd, instanceCounts.getOrDefault(ltd, 0) + 1);
        }
        slog.debugf("The types with more than 5 instances are: ");
        instanceCounts.entrySet().stream()
            .filter(x -> x.getValue() > 5)
            .sorted((x, y) -> y.getValue().compareTo(x.getValue()))
            .forEach(e -> slog.debugf("  %,6d instances of %s", e.getValue(), e.getKey().getDescriptor()));
    }

    void initializeRootClassArray(int numTypeIds) {
        LoadedTypeDefinition jlc = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load();
        CompoundType jlcType = layout.getInstanceLayoutInfo(jlc).getCompoundType();
        ArrayType rootArrayType = ctxt.getTypeSystem().getArrayType(jlcType, numTypeIds);
        rootClassesDecl = classSection.getProgramModule().declareData(null, "qbicc_jlc_lookup_table", rootArrayType);
        rootClasses = new Literal[numTypeIds];
        rootClasses[0] = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(jlc.getObjectType()); // TODO: Remove if we assign void typeId 0 instead of using 0 as an invalid typeId
    }

    void emitRootClassArray() {
        Data d = classSection.addData(null, rootClassesDecl.getName(), ctxt.getLiteralFactory().literalOf((ArrayType) rootClassesDecl.getValueType(), List.of(rootClasses)));
        d.setLinkage(Linkage.EXTERNAL);
        d.setAddrspace(COLLECTED);
    }

    void emitRootClassDictionaries(ArrayList<VmClass> rootClasses) {
        LoadedTypeDefinition ih = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/InitialHeap").load();
        ModuleSection section = ctxt.getImplicitSection(ih);
        LoadedTypeDefinition jls_td = ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load();
        LoadedTypeDefinition jlc_td = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load();
        VmClass jls = jls_td.getVmClass();
        VmClass jlc = jlc_td.getVmClass();
        int nameIdx= jlc.indexOf(jlc_td.findField("name"));

        rootClasses.sort(Comparator.comparing(x -> x.getTypeDefinition().getInternalName()));

        // Construct and serialize the sorted VmReferenceArray of Strings that are class names
        VmObject[] names = new VmObject[rootClasses.size()];
        for (int i=0; i<rootClasses.size(); i++) {
            names[i] = rootClasses.get(i).getMemory().loadRef(nameIdx, SinglePlain);
        }
        VmReferenceArray nameArray = ctxt.getVm().newArrayOf(jls, names);
        serializeVmObject(nameArray, false);
        String name1 = ih.getInternalName().replace('/', '.') + "." + ih.findField("bootstrapClassNames").getName();
        Data d1 = section.addData(null, name1,  referToSerializedVmObject(nameArray, nameArray.getObjectType().getReference(), section.getProgramModule()));
        d1.setLinkage(Linkage.EXTERNAL);
        d1.setDsoLocal();

        // Construct and serialize the sorted VmReferenceArray of Class instances
        VmReferenceArray classArray = ctxt.getVm().newArrayOf(jlc, rootClasses.toArray(new VmObject[rootClasses.size()]));
        serializeVmObject(classArray, false);
        String name2 = ih.getInternalName().replace('/', '.') + "." + ih.findField("bootstrapClasses").getName();
        Data d2 = section.addData(null, name2,  referToSerializedVmObject(classArray, classArray.getObjectType().getReference(), section.getProgramModule()));
        d2.setLinkage(Linkage.EXTERNAL);
        d2.setDsoLocal();
    }

    public ProgramObject getAndRegisterGlobalClassArray(ExecutableElement originalElement) {
        ProgramModule programModule = ctxt.getOrAddProgramModule(originalElement.getEnclosingType());
        DataDeclaration decl = programModule.declareData(rootClassesDecl);
        decl.setAddrspace(COLLECTED);
        return decl;
    }

    public boolean containsObject(VmObject value) {
        return vmObjects.containsKey(value);
    }

    public synchronized Literal referToSerializedVmObject(VmObject value, NullableType desiredType, ProgramModule from) {
        if (isRootClass(value)) {
            LiteralFactory lf  = ctxt.getLiteralFactory();
            DataDeclaration d = from.declareData(rootClassesDecl);
            d.setAddrspace(COLLECTED);
            int typeId = ((VmClass)value).getTypeDefinition().getTypeId();
            Literal base = lf.bitcastLiteral(lf.literalOf(ProgramObjectPointer.of(rootClassesDecl)), ((ArrayType)rootClassesDecl.getValueType()).getElementType().getPointer().asCollected());
            Literal elem = lf.elementOfLiteral(base, lf.literalOf(typeId));
            return ctxt.getLiteralFactory().bitcastLiteral(elem, desiredType);
        } else {
            DataDeclaration objDecl = vmObjects.get(value);
            if (objDecl == null) {
                ctxt.warning("Requested VmObject not found in build time heap: " + value);
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(desiredType);
            }
            DataDeclaration decl = from.declareData(objDecl);
            decl.setAddrspace(COLLECTED);
            return ctxt.getLiteralFactory().bitcastLiteral(ctxt.getLiteralFactory().literalOf(decl), desiredType);
        }
    }

    public synchronized void serializeVmObject(VmObject value, boolean toInternedStringSection) {
        if (toInternedStringSection) {
            serializeVmObject(value, stringSection);
        } else {
            serializeVmObject(value, objectSection);
        }
    }

    private void serializeVmObject(VmObject value, ModuleSection into) {
        if (vmObjects.containsKey(value)) {
            return;
        }
        if (isRootClass(value)) {
            int typeId = ((VmClass)value).getTypeDefinition().getTypeId();
            if (rootClasses[typeId] != null) {
                return;
            }
        }
        Layout layout = Layout.get(ctxt);
        PhysicalObjectType ot = value.getObjectType();
        if (ot instanceof ClassObjectType) {
            // Could be part of a cyclic object graph; must record the symbol for this object before we serialize its fields
            LoadedTypeDefinition concreteType = ot.getDefinition().load();
            LayoutInfo objLayout = layout.getInstanceLayoutInfo(concreteType);
            if (isRootClass(value)) {
                int typeId = ((VmClass)value).getTypeDefinition().getTypeId();
                rootClasses[typeId] = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(value.getObjectType()); // indicate serialization has started
                serializeVmObject(concreteType, objLayout, value, classSection, typeId, null); // now serialize and update rootClass[typeId]
            } else {
                if (into == objectSection && ctxt.getVm().isInternedString(value)) {
                    // Detect when the first reference to an interned String is from some arbitrary heap object and override section
                    into = stringSection;
                }
                String name = nextLiteralName(into);
                DataDeclaration decl = into.getProgramModule().declareData(null, name, objLayout.getCompoundType());
                decl.setAddrspace(COLLECTED);
                vmObjects.put(value, decl); // record declaration
                serializeVmObject(concreteType, objLayout, value, into, -1, decl.getName()); // now serialize and define a Data
            }
        } else if (ot instanceof ReferenceArrayObjectType) {
            // Could be part of a cyclic object graph; must record the symbol for this array before we serialize its elements
            FieldElement contentsField = coreClasses.getRefArrayContentField();
            LayoutInfo info = layout.getInstanceLayoutInfo(contentsField.getEnclosingType());
            Memory memory = value.getMemory();
            int length = memory.load32(info.getMember(coreClasses.getArrayLengthField()).getOffset(), SinglePlain);
            CompoundType literalCT = arrayLiteralType(contentsField, length);
            DataDeclaration decl = into.getProgramModule().declareData(null, nextLiteralName(into), literalCT);
            decl.setAddrspace(COLLECTED);
            vmObjects.put(value, decl); // record declaration
            serializeRefArray((ReferenceArrayObjectType) ot, literalCT, length, into, decl, (VmArray)value); // now serialize
        } else {
            // Can't be part of cyclic structure; don't need to record declaration first
            vmObjects.put(value, serializePrimArray((PrimitiveArrayObjectType) ot, (VmArray)value, into));
        }
    }

    private boolean isRootClass(VmObject value) {
        return value instanceof VmClass vmClass && !(vmClass instanceof VmReferenceArrayClass) && vmClass.getTypeDefinition().getTypeId() != -1;
    }

    private String nextLiteralName(ModuleSection into) {
        if (into == objectSection) {
            return "qbicc_initial_heap_obj_" + (this.literalCounter++);
        } else {
            return "qbicc_initial_heap_iss_" + (this.literalCounter++);
        }
    }

    private Data defineData(ModuleSection into, String name, Literal value) {
        Data d = into.addData(null, name, value);
        d.setLinkage(Linkage.EXTERNAL);
        d.setAddrspace(COLLECTED);
        return d;
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

    private void serializeVmObject(LoadedTypeDefinition concreteType, LayoutInfo objLayout, VmObject value, ModuleSection into, int typeId, String name) {
        Memory memory = value.getMemory();
        LayoutInfo memLayout = layout.getInstanceLayoutInfo(concreteType);
        CompoundType objType = objLayout.getCompoundType();
        HashMap<CompoundType.Member, Literal> memberMap = new HashMap<>();

        populateMemberMap(concreteType, objType, objLayout, memLayout, memory, memberMap, into);

        // Define it!
        if (typeId == -1) {
            defineData(into, name, ctxt.getLiteralFactory().literalOf(objType, memberMap));
        } else {
            rootClasses[typeId] = ctxt.getLiteralFactory().literalOf(objType, memberMap);
        }
    }

    private void populateMemberMap(final LoadedTypeDefinition concreteType, final CompoundType objType, final LayoutInfo objLayout, final LayoutInfo memLayout, final Memory memory,
                                   final HashMap<CompoundType.Member, Literal> memberMap, ModuleSection into) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        // Start by zero-initializing all members
        for (CompoundType.Member m : objType.getMembers()) {
            memberMap.put(m, lf.zeroInitializerLiteralOfType(m.getType()));
        }

        populateClearedMemberMap(concreteType, objLayout, memLayout, memory, memberMap, into);
    }

    private void populateClearedMemberMap(final LoadedTypeDefinition concreteType, final LayoutInfo objLayout, final LayoutInfo memLayout, final Memory memory,
                                          final HashMap<CompoundType.Member, Literal> memberMap, ModuleSection into) {
        if (concreteType.hasSuperClass()) {
            populateClearedMemberMap(concreteType.getSuperClass(), objLayout, memLayout, memory, memberMap, into);
        }

        LiteralFactory lf = ctxt.getLiteralFactory();
        // Iterate over declared instance fields and copy values from the backing Memory to the memberMap
        int fc = concreteType.getFieldCount();
        for (int i=0; i<fc; i++) {
            FieldElement f = concreteType.getField(i);
            if (f.isStatic()) {
                continue;
            }

            CompoundType.Member im = memLayout.getMember(f);
            CompoundType.Member om = objLayout.getMember(f);
            if (im == null || om == null) {
                ctxt.warning("Field " + f +" not serialized due to incomplete layout");
                continue;
            }
            Literal replacement = f.getReplacementValue(ctxt);
            if (replacement != null) {
                if (replacement instanceof BooleanLiteral bl) {
                    replacement = lf.literalOf((IntegerType)om.getType(), bl.booleanValue() ? 1 : 0);
                }
                memberMap.put(om, replacement);
            } else if (im.getType() instanceof IntegerType it) {
                if (it.getSize() == 1) {
                    memberMap.put(om, lf.literalOf(it, memory.load8(im.getOffset(), SinglePlain)));
                } else if (it.getSize() == 2) {
                    memberMap.put(om, lf.literalOf(it, memory.load16(im.getOffset(), SinglePlain)));
                } else if (it.getSize() == 4) {
                    memberMap.put(om, lf.literalOf(it, memory.load32(im.getOffset(), SinglePlain)));
                } else {
                    memberMap.put(om, lf.literalOf(it, memory.load64(im.getOffset(), SinglePlain)));
                }
            } else if (im.getType() instanceof FloatType ft) {
                if (ft.getSize() == 4) {
                    memberMap.put(om, lf.literalOf(ft, memory.loadFloat(im.getOffset(), SinglePlain)));
                } else {
                    memberMap.put(om, lf.literalOf(ft, memory.loadDouble(im.getOffset(), SinglePlain)));
                }
            } else if (im.getType() instanceof TypeType) {
                ValueType type = memory.loadType(im.getOffset(), SinglePlain);
                memberMap.put(om, type == null ? lf.zeroInitializerLiteralOfType(im.getType()) : lf.literalOfType(type));
            } else if (im.getType() instanceof ArrayType) {
                if (im.getType().getSize() > 0) {
                    throw new UnsupportedOperationException("Copying array data is not yet supported");
                }
            } else if (im.getType() instanceof ReferenceType rt) {
                VmObject contents = memory.loadRef(im.getOffset(), SinglePlain);
                if (contents == null) {
                    memberMap.put(om, lf.zeroInitializerLiteralOfType(om.getType()));
                } else {
                    serializeVmObject(contents, into == classSection ? objectSection : into);
                    memberMap.put(om, referToSerializedVmObject(contents, rt, into.getProgramModule()));
                }
            } else if (im.getType() instanceof PointerType pt) {
                Pointer pointer = memory.loadPointer(im.getOffset(), SinglePlain);
                if (pointer == null) {
                    memberMap.put(om, lf.nullLiteralOfType(pt));
                } else if (pointer instanceof StaticMethodPointer smp) {
                    // lower method pointers to their corresponding objects
                    MethodElement method = smp.getStaticMethod();
                    ctxt.enqueue(method);
                    Function function = ctxt.getExactFunction(method);
                    FunctionDeclaration decl = into.getProgramModule().declareFunction(function);
                    memberMap.put(om, lf.bitcastLiteral(lf.literalOf(ProgramObjectPointer.of(decl)), smp.getType()));
                } else {
                    memberMap.put(om, lf.literalOf(pointer));
                }
            } else {
                ctxt.warning("Serializing " + f + " as zero literal. Unsupported type");
                memberMap.put(om, lf.zeroInitializerLiteralOfType(im.getType()));
            }
        }
    }

    private void serializeRefArray(ReferenceArrayObjectType at, CompoundType literalCT, int length, ModuleSection into, DataDeclaration sl, VmArray value) {
        LoadedTypeDefinition jlo = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load();
        LiteralFactory lf = ctxt.getLiteralFactory();

        Layout layout = Layout.get(ctxt);
        Memory memory = value.getMemory();
        FieldElement contentField = coreClasses.getRefArrayContentField();
        DefinedTypeDefinition concreteType = contentField.getEnclosingType();
        LayoutInfo objLayout = layout.getInstanceLayoutInfo(concreteType);
        LayoutInfo memLayout = this.layout.getInstanceLayoutInfo(concreteType);
        CompoundType objType = objLayout.getCompoundType();
        HashMap<CompoundType.Member, Literal> memberMap = new HashMap<>();

        populateMemberMap(concreteType.load(), objType, objLayout, memLayout, memory, memberMap, into);

        List<Literal> elements = new ArrayList<>(length);
        VmObject[] elementArray = ((VmReferenceArray) value).getArray();
        for (int i=0; i<length; i++) {
            VmObject e = elementArray[i];
            if (e == null) {
                elements.add(lf.zeroInitializerLiteralOfType(at.getElementType()));
            } else {
                serializeVmObject(e, into);
                elements.add(referToSerializedVmObject(e, jlo.getClassType().getReference(), into.getProgramModule()));
            }
        }

        ArrayType arrayType = ctxt.getTypeSystem().getArrayType(jlo.getClassType().getReference(), length);

        // add the actual array contents
        memberMap.put(literalCT.getMember(literalCT.getMemberCount() - 1), lf.literalOf(arrayType, elements));

        // Define it with the literal type we generated above
        defineData(into, sl.getName(), ctxt.getLiteralFactory().literalOf(literalCT, memberMap));
    }

    private DataDeclaration serializePrimArray(PrimitiveArrayObjectType at, VmArray value, ModuleSection into) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        Layout layout = Layout.get(ctxt);
        FieldElement contentsField = coreClasses.getArrayContentField(at);
        DefinedTypeDefinition concreteType = contentsField.getEnclosingType();
        LayoutInfo objLayout = layout.getInstanceLayoutInfo(concreteType);
        LayoutInfo memLayout = this.layout.getInstanceLayoutInfo(concreteType);
        CompoundType objType = objLayout.getCompoundType();

        Memory memory = value.getMemory();
        int length = memory.load32(objLayout.getMember(coreClasses.getArrayLengthField()).getOffset(), SinglePlain);
        CompoundType literalCT = arrayLiteralType(contentsField, length);

        Literal arrayContentsLiteral;
        if (contentsField.equals(coreClasses.getByteArrayContentField())) {
            byte[] contents = (byte[]) value.getArray();
            arrayContentsLiteral = lf.literalOf(ctxt.getTypeSystem().getArrayType(at.getElementType(), length), contents);
        } else {
            List<Literal> elements = new ArrayList<>(length);
            if (contentsField.equals(coreClasses.getBooleanArrayContentField())) {
                boolean[] contents = (boolean[]) value.getArray();
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(contents[i]));
                }
            } else if (contentsField.equals(coreClasses.getShortArrayContentField())) {
                short[] contents = (short[]) value.getArray();
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(contents[i]));
                }
            } else if (contentsField.equals(coreClasses.getCharArrayContentField())) {
                char[] contents = (char[]) value.getArray();
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(contents[i]));
                }
            } else if (contentsField.equals(coreClasses.getIntArrayContentField())) {
                int[] contents = (int[]) value.getArray();
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(contents[i]));
                }
            } else if (contentsField.equals(coreClasses.getLongArrayContentField())) {
                long[] contents = (long[]) value.getArray();
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(contents[i]));
                }
            } else if (contentsField.equals(coreClasses.getFloatArrayContentField())) {
                float[] contents = (float[]) value.getArray();
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(contents[i]));
                }
            } else {
                Assert.assertTrue((contentsField.equals(coreClasses.getDoubleArrayContentField())));
                double[] contents = (double[]) value.getArray();
                for (int i=0; i<length; i++) {
                    elements.add(lf.literalOf(contents[i]));
                }
            }
            arrayContentsLiteral = lf.literalOf(ctxt.getTypeSystem().getArrayType(at.getElementType(), length), elements);
        }

        HashMap<CompoundType.Member, Literal> memberMap = new HashMap<>();

        populateMemberMap(concreteType.load(), objType, objLayout, memLayout, memory, memberMap, into);

        // add the actual array contents
        memberMap.put(literalCT.getMember(literalCT.getMemberCount() - 1), arrayContentsLiteral);

        Data arrayData = defineData(into, nextLiteralName(into), ctxt.getLiteralFactory().literalOf(literalCT, memberMap));
        DataDeclaration decl = arrayData.getDeclaration();
        decl.setAddrspace(COLLECTED);
        return decl;
    }
}

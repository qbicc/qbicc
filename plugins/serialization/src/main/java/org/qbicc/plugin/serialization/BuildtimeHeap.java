package org.qbicc.plugin.serialization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.smallrye.common.constraint.Assert;
import org.jboss.logging.Logger;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.OffsetOfField;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmReferenceArrayClass;
import org.qbicc.interpreter.memory.ByteArrayMemory;
import org.qbicc.machine.arch.Platform;
import org.qbicc.object.Data;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.Linkage;
import org.qbicc.object.ModuleSection;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.ProgramObject;
import org.qbicc.object.Section;
import org.qbicc.object.Segment;
import org.qbicc.plugin.constants.Constants;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.plugin.threadlocal.ThreadLocals;
import org.qbicc.pointer.GlobalPointer;
import org.qbicc.pointer.IntegerAsPointer;
import org.qbicc.pointer.MemoryPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.pointer.ProgramObjectPointer;
import org.qbicc.pointer.StaticFieldPointer;
import org.qbicc.pointer.StaticMethodPointer;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.StructType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.NullableType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeIdType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.definition.element.StaticMethodElement;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeSignature;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

public class BuildtimeHeap {
    private static final AttachmentKey<BuildtimeHeap> KEY = new AttachmentKey<>();
    private static final Logger slog = Logger.getLogger("org.qbicc.plugin.serialization.stats");

    private final CompilationContext ctxt;
    private final Layout layout;
    private final CoreClasses coreClasses;
    /**
     * For lazy definition of native array types for literals
     */
    private final HashMap<String, StructType> arrayTypes = new HashMap<>();
    /**
     * For interning VmObjects
     */
    private final IdentityHashMap<VmObject, DataDeclaration> vmObjects = new IdentityHashMap<>();
    /**
     * For interning native memory
     */
    private final HashMap<GlobalVariableElement, DataDeclaration> nativeMemory = new HashMap<>();
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
     * The section where references are stored, where they can be found easily by GC.
     */
    private final Section refSection;
    /**
     * The global array of root java.lang.Class instances
     */
    private DataDeclaration rootClassesDecl;
    /**
     * The values, indexed by typeId to be serialized in the classArrayGlobal
     */
    private Literal[] rootClasses;
    /**
     * The mapping from static fields to global variables
     */
    private final Map<FieldElement, GlobalVariableElement> staticFields = new ConcurrentHashMap<>();

    private int literalCounter = 0;

    private BuildtimeHeap(CompilationContext ctxt) {
        this.ctxt = ctxt;
        this.layout = Layout.get(ctxt);
        this.coreClasses = CoreClasses.get(ctxt);

        Platform p = ctxt.getPlatform();
        refSection = Section.defineSection(ctxt, 0, "refs", Segment.DATA, Section.Flag.DATA_ONLY);
        Section classSection = Section.defineSection(ctxt, 3, "classes", Segment.DATA, Section.Flag.DATA_ONLY);
        // todo: class objects belong in their corresponding modules
        this.classSection = ctxt.getOrAddProgramModule(ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/InitialHeap$ClassSection").load()).inSection(classSection);
        Section stringSection = Section.defineSection(ctxt, 4, "strings", Segment.DATA, Section.Flag.DATA_ONLY);
        this.stringSection = ctxt.getOrAddProgramModule(ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/InitialHeap$InternedStringSection").load()).inSection(stringSection);
        Section objectSection = Section.defineSection(ctxt, 4, "objects", Segment.DATA, Section.Flag.DATA_ONLY);
        this.objectSection = ctxt.getOrAddProgramModule(ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/InitialHeap$ObjectSection").load()).inSection(objectSection);
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
        StructType jlcType = layout.getInstanceLayoutInfo(jlc).getStructType();
        ArrayType rootArrayType = ctxt.getTypeSystem().getArrayType(jlcType, numTypeIds);
        rootClassesDecl = classSection.getProgramModule().declareData(null, "qbicc_jlc_lookup_table", rootArrayType);
        rootClasses = new Literal[numTypeIds];
        rootClasses[0] = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(jlc.getObjectType()); // TODO: Remove if we assign void typeId 0 instead of using 0 as an invalid typeId
    }

    void emitRootClassArray() {
        Data d = classSection.addData(null, rootClassesDecl.getName(), ctxt.getLiteralFactory().literalOf((ArrayType) rootClassesDecl.getValueType(), List.of(rootClasses)));
        d.setLinkage(Linkage.EXTERNAL);
    }

    void emitRootClassDictionaries(ArrayList<VmClass> reachableClasses) {
        LoadedTypeDefinition ih = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/InitialHeap").load();
        ModuleSection section = ctxt.getImplicitSection(ih);
        LoadedTypeDefinition jls_td = ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load();
        LoadedTypeDefinition jlc_td = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load();
        LoadedTypeDefinition jlcl_td = ctxt.getBootstrapClassContext().findDefinedType("java/lang/ClassLoader").load();
        VmClass jls = jls_td.getVmClass();
        VmClass jlc = jlc_td.getVmClass();
        VmClass jlcl = jlcl_td.getVmClass();
        VmClassLoader bootLoader = ctxt.getBootstrapClassContext().getClassLoader();

        // Group the reachable classes by their defining loader.
        HashMap<VmClassLoader, ArrayList<VmClass>> clMap = new HashMap<>();
        for (VmClass c: reachableClasses) {
            VmClassLoader cl = c.getClassLoader();
            if (cl == null) {
                cl = bootLoader;
            }
            clMap.computeIfAbsent(cl, k -> new ArrayList<>()).add(c);
        }

        // Build the dictionaries.
        int numClassLoaders = clMap.size();
        VmClassLoader[] classLoaders = new VmClassLoader[numClassLoaders];
        VmReferenceArray[] names = new VmReferenceArray[numClassLoaders];
        VmReferenceArray[] classes = new VmReferenceArray[numClassLoaders];
        classLoaders[0] = bootLoader;
        int nextSlot = 1;
        for (VmClassLoader cl: clMap.keySet()) {
            if (cl != bootLoader) {
                classLoaders[nextSlot++] = cl;
            }
        }
        int nameIdx= jlc.indexOf(jlc_td.findField("name"));
        for (int clIndex = 0; clIndex<numClassLoaders; clIndex++) {
            ArrayList<VmClass> myDefines = clMap.get(classLoaders[clIndex]);
            myDefines.sort(Comparator.comparing(x -> x.getTypeDefinition().getInternalName()));
            VmObject[] myNames = new VmObject[myDefines.size()];
            VmObject[] myClasses = new VmObject[myDefines.size()];
            for (int i=0; i<myDefines.size(); i++) {
                myClasses[i] = myDefines.get(i);
                myNames[i] = myClasses[i].getMemory().loadRef(nameIdx, SinglePlain);
            }
            classes[clIndex] = ctxt.getVm().newArrayOf(jlc, myClasses);
            names[clIndex] = ctxt.getVm().newArrayOf(jls, myNames);
        }
        VmReferenceArray loaders = ctxt.getVm().newArrayOf(jlcl, classLoaders);
        VmReferenceArray nameSpine = ctxt.getVm().newArrayOf(jls.getArrayClass(), names);
        VmReferenceArray classSpine = ctxt.getVm().newArrayOf(jlc.getArrayClass(), classes);

        // Serialize them
        serializeVmObject(loaders, false);
        serializeVmObject(nameSpine, false);
        serializeVmObject(classSpine, false);

        String fn = ih.getInternalName().replace('/', '.') + "." + ih.findField("classLoaders").getName();
        Data d = section.addData(null, fn,  referToSerializedVmObject(loaders, loaders.getObjectType().getReference(), section.getProgramModule()));
        d.setLinkage(Linkage.EXTERNAL);
        d.setDsoLocal();
        fn = ih.getInternalName().replace('/', '.') + "." + ih.findField("classNames").getName();
        d = section.addData(null, fn,  referToSerializedVmObject(nameSpine, nameSpine.getObjectType().getReference(), section.getProgramModule()));
        d.setLinkage(Linkage.EXTERNAL);
        d.setDsoLocal();
        fn = ih.getInternalName().replace('/', '.') + "." + ih.findField("classes").getName();
        d = section.addData(null, fn,  referToSerializedVmObject(classSpine, classSpine.getObjectType().getReference(), section.getProgramModule()));
        d.setLinkage(Linkage.EXTERNAL);
        d.setDsoLocal();
    }

    public ProgramObject getAndRegisterGlobalClassArray(ExecutableElement originalElement) {
        ProgramModule programModule = ctxt.getOrAddProgramModule(originalElement.getEnclosingType());
        DataDeclaration decl = programModule.declareData(rootClassesDecl);
        return decl;
    }

    public GlobalVariableElement getGlobalForStaticField(StaticFieldElement field) {
        GlobalVariableElement global = staticFields.get(field);
        if (global != null) {
            return global;
        }
        DefinedTypeDefinition typeDef = field.getEnclosingType();
        ClassContext classContext = typeDef.getContext();
        CompilationContext ctxt = classContext.getCompilationContext();
        TypeDescriptor fieldDesc = field.getTypeDescriptor();
        String globalName = field.getLoweredName();
        final GlobalVariableElement.Builder builder = GlobalVariableElement.builder(globalName, fieldDesc);
        ValueType globalType = widenBoolean(field.getType());
        builder.setType(globalType);
        builder.setSignature(TypeSignature.synthesize(classContext, fieldDesc));
        builder.setModifiers(field.getModifiers());
        builder.setEnclosingType(typeDef);
        Section section = field.getType() instanceof ReferenceType ? refSection : ctxt.getImplicitSection();
        builder.setSection(section);
        builder.setMinimumAlignment(field.getMinimumAlignment());
        global = builder.build();
        GlobalVariableElement appearing = staticFields.putIfAbsent(field, global);
        if (appearing != null) {
            return appearing;
        }
        // Sleazy hack.  The static fields of the InitialHeap class will be declared during serialization.
        if (typeDef.internalPackageAndNameEquals("org/qbicc/runtime/main", "InitialHeap")) {
            return global;
        }
        // we added it, so we must add the definition as well
        LiteralFactory lf = ctxt.getLiteralFactory();
        ModuleSection moduleSection = ctxt.getOrAddProgramModule(typeDef).inSection(section);
        Value initialValue;
        if (field.getRunTimeInitializer() != null) {
            initialValue = lf.zeroInitializerLiteralOfType(globalType);
        } else {
            initialValue = field.getReplacementValue(ctxt);
            if (initialValue == null) {
                initialValue = typeDef.load().getInitialValue(field);
            }
            if (initialValue == null) {
                initialValue = Constants.get(ctxt).getConstantValue(field);
                if (initialValue == null) {
                    initialValue = lf.zeroInitializerLiteralOfType(globalType);
                }
            }
        }
        if (initialValue instanceof OffsetOfField oof) {
            // special case: the field holds an offset which is really an integer
            FieldElement offsetField = oof.getFieldElement();
            LayoutInfo instanceLayout = Layout.get(ctxt).getInstanceLayoutInfo(offsetField.getEnclosingType());
            if (offsetField.isStatic()) {
                initialValue = lf.literalOf(0);
            } else {
                initialValue = lf.literalOf(instanceLayout.getMember(offsetField).getOffset());
            }
        }
        if (initialValue.getType() instanceof BooleanType && globalType instanceof IntegerType it) {
            // widen the initial value
            if (initialValue instanceof BooleanLiteral) {
                initialValue = lf.literalOf(it, ((BooleanLiteral) initialValue).booleanValue() ? 1 : 0);
            } else if (initialValue instanceof ZeroInitializerLiteral) {
                initialValue = lf.literalOf(it, 0);
            } else {
                throw new IllegalArgumentException("Cannot initialize boolean field");
            }
        }
        if (initialValue instanceof ObjectLiteral ol) {
            BuildtimeHeap bth = BuildtimeHeap.get(ctxt);
            bth.serializeVmObject(ol.getValue(), false);
            initialValue = bth.referToSerializedVmObject(ol.getValue(), ol.getType(), moduleSection.getProgramModule());
        }
        if (initialValue.getType() instanceof ReferenceType irt && field.getType() instanceof ReferenceType frt && !irt.equals(frt)) {
            // we need a widening conversion
            initialValue = lf.bitcastLiteral((Literal) initialValue, frt);
        }
        final Data data = moduleSection.addData(field, globalName, initialValue);
        data.setLinkage(Linkage.EXTERNAL);
        data.setDsoLocal();
        return global;
    }

    private ValueType widenBoolean(ValueType type) {
        // todo: n-bit booleans
        if (type instanceof BooleanType) {
            TypeSystem ts = type.getTypeSystem();
            return ts.getUnsignedInteger8Type();
        } else if (type instanceof ArrayType arrayType) {
            TypeSystem ts = type.getTypeSystem();
            ValueType elementType = arrayType.getElementType();
            ValueType widened = widenBoolean(elementType);
            return elementType == widened ? type : ts.getArrayType(widened, arrayType.getElementCount());
        } else {
            return type;
        }
    }

    public boolean containsObject(VmObject value) {
        return vmObjects.containsKey(value);
    }

    public synchronized Literal referToSerializedVmObject(VmObject value, NullableType desiredType, ProgramModule from) {
        LiteralFactory lf  = ctxt.getLiteralFactory();
        if (isRootClass(value)) {
            DataDeclaration d = from.declareData(rootClassesDecl);
            int typeId = ((VmClass)value).getTypeDefinition().getTypeId();
            // todo: change to offset-from classes segment
            Literal elem = lf.elementOfLiteral(lf.literalOf(d), lf.literalOf(typeId));
            if (desiredType instanceof ReferenceType rt) {
                return lf.encodeReferenceLiteral(elem, rt);
            } else {
                return lf.bitcastLiteral(elem, desiredType);
            }
        } else {
            DataDeclaration objDecl = vmObjects.get(value);
            if (objDecl == null) {
                ctxt.warning("Requested VmObject not found in build time heap: " + value);
                return lf.nullLiteralOfType(desiredType);
            }
            DataDeclaration decl = from.declareData(objDecl);
            if (desiredType instanceof ReferenceType rt) {
                return lf.encodeReferenceLiteral(lf.literalOf(decl), rt);
            } else {
                return lf.bitcastLiteral(lf.literalOf(decl), desiredType);
            }
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
            if (concreteType.getTypeId() == -1) {
                ctxt.warning("Serialized an instance of %s whose typeId is -1 (unreachable type)", concreteType.getDescriptor().toString());
            }
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
                DataDeclaration decl = into.getProgramModule().declareData(null, name, objLayout.getStructType());
                vmObjects.put(value, decl); // record declaration
                serializeVmObject(concreteType, objLayout, value, into, -1, decl.getName()); // now serialize and define a Data
            }
        } else if (ot instanceof ReferenceArrayObjectType) {
            // Could be part of a cyclic object graph; must record the symbol for this array before we serialize its elements
            FieldElement contentsField = coreClasses.getRefArrayContentField();
            LayoutInfo info = layout.getInstanceLayoutInfo(contentsField.getEnclosingType());
            Memory memory = value.getMemory();
            int length = memory.load32(info.getMember(coreClasses.getArrayLengthField()).getOffset(), SinglePlain);
            StructType literalCT = arrayLiteralType(contentsField, length);
            DataDeclaration decl = into.getProgramModule().declareData(null, nextLiteralName(into), literalCT);
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
        return d;
    }

    private StructType arrayLiteralType(FieldElement contents, int length) {
        LoadedTypeDefinition ltd = contents.getEnclosingType().load();
        String typeName = ltd.getInternalName() + "_" + length;
        StructType sizedArrayType = arrayTypes.get(typeName);
        Layout layout = Layout.get(ctxt);
        if (sizedArrayType == null) {
            TypeSystem ts = ctxt.getTypeSystem();
            LayoutInfo objLayout = layout.getInstanceLayoutInfo(ltd);
            StructType arrayCT = objLayout.getStructType();

            StructType.Member contentMem = objLayout.getMember(contents);
            ArrayType sizedContentMem = ts.getArrayType(((ArrayType) contents.getType()).getElementType(), length);
            StructType.Member realContentMem = ts.getStructTypeMember(contentMem.getName(), sizedContentMem, contentMem.getOffset(), contentMem.getAlign());

            Supplier<List<StructType.Member>> thunk = () -> {
                StructType.Member[] items = arrayCT.getMembers().toArray(StructType.Member[]::new);
                for (int i = 0; i < items.length; i++) {
                    if (items[i] == contentMem) {
                        items[i] = realContentMem;
                    }
                }
                return Arrays.asList(items);
            };

            sizedArrayType = ts.getStructType(StructType.Tag.STRUCT, typeName, arrayCT.getSize() + sizedContentMem.getSize(), arrayCT.getAlign(), thunk);
            arrayTypes.put(typeName, sizedArrayType);
        }
        return sizedArrayType;
    }

    private void serializeVmObject(LoadedTypeDefinition concreteType, LayoutInfo objLayout, VmObject value, ModuleSection into, int typeId, String name) {
        Memory memory = value.getMemory();
        LayoutInfo memLayout = layout.getInstanceLayoutInfo(concreteType);
        StructType objType = objLayout.getStructType();
        HashMap<StructType.Member, Literal> memberMap = new HashMap<>();

        populateMemberMap(concreteType, objType, objLayout, memLayout, memory, memberMap, into);

        // Define it!
        if (typeId == -1) {
            defineData(into, name, ctxt.getLiteralFactory().literalOf(objType, memberMap));
        } else {
            rootClasses[typeId] = ctxt.getLiteralFactory().literalOf(objType, memberMap);
        }
    }

    private void populateMemberMap(final LoadedTypeDefinition concreteType, final StructType objType, final LayoutInfo objLayout, final LayoutInfo memLayout, final Memory memory,
                                   final HashMap<StructType.Member, Literal> memberMap, ModuleSection into) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        // Start by zero-initializing all members
        for (StructType.Member m : objType.getMembers()) {
            memberMap.put(m, lf.zeroInitializerLiteralOfType(m.getType()));
        }

        populateClearedMemberMap(concreteType, objLayout, memLayout, memory, memberMap, into);
    }

    private void populateClearedMemberMap(final LoadedTypeDefinition concreteType, final LayoutInfo objLayout, final LayoutInfo memLayout, final Memory memory,
                                          final HashMap<StructType.Member, Literal> memberMap, ModuleSection into) {
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

            StructType.Member im = memLayout.getMember(f);
            StructType.Member om = objLayout.getMember(f);
            if (im == null || om == null) {
                if (!ThreadLocals.get(ctxt).isThreadLocalField((InstanceFieldElement)f)) {
                    ctxt.warning("Field " + f + " not serialized due to incomplete layout");
                }
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
                    Pointer asPointerVal = memory.loadPointer(im.getOffset(), SinglePlain);
                    if (asPointerVal instanceof IntegerAsPointer iap) {
                        memberMap.put(om, lf.literalOf(it, iap.getValue()));
                    } else if (asPointerVal == null) {
                        memberMap.put(om, lf.literalOf(it, 0));
                    } else if (asPointerVal instanceof StaticMethodPointer smp) {
                        // lower method pointers to their corresponding objects
                        StaticMethodElement method = smp.getStaticMethod();
                        ctxt.enqueue(method);
                        Function function = ctxt.getExactFunction(method);
                        FunctionDeclaration decl = into.getProgramModule().declareFunction(function);
                        memberMap.put(om, lf.bitcastLiteral(lf.literalOf(ProgramObjectPointer.of(decl)), it));
                    } else if (asPointerVal instanceof StaticFieldPointer sfp) {
                        // lower static field pointers to their corresponding program objects
                        StaticFieldElement sfe = sfp.getStaticField();
                        GlobalVariableElement global = getGlobalForStaticField(sfe);
                        DataDeclaration decl = into.getProgramModule().declareData(sfe, global.getName(), global.getType());
                        memberMap.put(om, lf.bitcastLiteral(lf.literalOf(ProgramObjectPointer.of(decl)), it));
                    } else if (asPointerVal instanceof GlobalPointer gp) {
                        Memory m = ctxt.getVm().getGlobal(gp.getGlobalVariable());
                        if (m instanceof ByteArrayMemory bam) {
                            // Support serializing "native" memory allocated by Unsafe.allocateMemory0
                            DataDeclaration memDecl = serializeNativeMemory(gp.getGlobalVariable(), bam.getArray(), objectSection);
                            memberMap.put(om, lf.bitcastLiteral(lf.literalOf(memDecl), it));
                        } else {
                            ctxt.error(f.getLocation(), "An object contains an unlowerable pointer: %s", gp);
                        }
                    } else {
                        memberMap.put(om, lf.bitcastLiteral(lf.literalOf(asPointerVal), it));
                    }
                }
            } else if (im.getType() instanceof FloatType ft) {
                if (ft.getSize() == 4) {
                    memberMap.put(om, lf.literalOf(ft, memory.loadFloat(im.getOffset(), SinglePlain)));
                } else {
                    memberMap.put(om, lf.literalOf(ft, memory.loadDouble(im.getOffset(), SinglePlain)));
                }
            } else if (im.getType() instanceof TypeIdType) {
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
                    StaticMethodElement method = smp.getStaticMethod();
                    ctxt.enqueue(method);
                    Function function = ctxt.getExactFunction(method);
                    FunctionDeclaration decl = into.getProgramModule().declareFunction(function);
                    memberMap.put(om, lf.bitcastLiteral(lf.literalOf(ProgramObjectPointer.of(decl)), pt));
                } else if (pointer instanceof StaticFieldPointer sfp) {
                    // lower static field pointers to their corresponding program objects
                    StaticFieldElement sfe = sfp.getStaticField();
                    GlobalVariableElement global = getGlobalForStaticField(sfe);
                    DataDeclaration decl = into.getProgramModule().declareData(sfe, global.getName(), global.getType());
                    memberMap.put(om, lf.bitcastLiteral(lf.literalOf(ProgramObjectPointer.of(decl)), sfp.getType()));
                } else if (pointer instanceof MemoryPointer mp) {
                    ctxt.error(f.getLocation(), "An object contains a memory pointer: %s", mp);
                } else {
                    memberMap.put(om, lf.literalOf(pointer));
                }
            } else {
                ctxt.warning("Serializing " + f + " as zero literal. Unsupported type");
                memberMap.put(om, lf.zeroInitializerLiteralOfType(im.getType()));
            }
        }
    }

    private void serializeRefArray(ReferenceArrayObjectType at, StructType literalCT, int length, ModuleSection into, DataDeclaration sl, VmArray value) {
        LoadedTypeDefinition jlo = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load();
        LiteralFactory lf = ctxt.getLiteralFactory();

        Layout layout = Layout.get(ctxt);
        Memory memory = value.getMemory();
        FieldElement contentField = coreClasses.getRefArrayContentField();
        DefinedTypeDefinition concreteType = contentField.getEnclosingType();
        LayoutInfo objLayout = layout.getInstanceLayoutInfo(concreteType);
        LayoutInfo memLayout = this.layout.getInstanceLayoutInfo(concreteType);
        StructType objType = objLayout.getStructType();
        HashMap<StructType.Member, Literal> memberMap = new HashMap<>();

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
        StructType objType = objLayout.getStructType();

        Memory memory = value.getMemory();
        int length = memory.load32(objLayout.getMember(coreClasses.getArrayLengthField()).getOffset(), SinglePlain);
        StructType literalCT = arrayLiteralType(contentsField, length);

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

        HashMap<StructType.Member, Literal> memberMap = new HashMap<>();

        populateMemberMap(concreteType.load(), objType, objLayout, memLayout, memory, memberMap, into);

        // add the actual array contents
        memberMap.put(literalCT.getMember(literalCT.getMemberCount() - 1), arrayContentsLiteral);

        Data arrayData = defineData(into, nextLiteralName(into), ctxt.getLiteralFactory().literalOf(literalCT, memberMap));
        return arrayData.getDeclaration();
    }

    private DataDeclaration serializeNativeMemory(GlobalVariableElement globalVariable, byte[] bytes, ModuleSection into) {
        DataDeclaration existing = nativeMemory.get(globalVariable);
        if (existing != null) {
            return existing;
        }
        LiteralFactory lf = ctxt.getLiteralFactory();
        Literal memoryLiteral = lf.literalOf(ctxt.getTypeSystem().getArrayType(ctxt.getTypeSystem().getSignedInteger8Type(), bytes.length), bytes);
        Data memoryData = defineData(into, globalVariable.getName(), memoryLiteral);
        DataDeclaration decl = memoryData.getDeclaration();
        nativeMemory.put(globalVariable, decl);
        return decl;
    }
}

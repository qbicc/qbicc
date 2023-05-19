package org.qbicc.interpreter.impl;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.common.constraint.Assert;
import org.jboss.logging.Logger;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.interpreter.InvalidMemoryAccessException;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmInvokable;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmPrimitiveClass;
import org.qbicc.interpreter.VmReferenceArrayClass;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThrowable;
import org.qbicc.interpreter.memory.MemoryFactory;
import org.qbicc.object.Data;
import org.qbicc.object.Linkage;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.pointer.IntegerAsPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.StructType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.descriptor.TypeDescriptor;

import static org.qbicc.graph.atomic.AccessModes.*;

class VmClassImpl extends VmObjectImpl implements VmClass {
    private static final Logger log = Logger.getLogger("org.qbicc.interpreter.initialization");

    private static final VarHandle interfacesHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "interfaces", VarHandle.class, VmClassImpl.class, List.class);

    private final VmImpl vm;
    /**
     * This is the type definition corresponding to the class represented by this instance.
     */
    private final LoadedTypeDefinition typeDefinition;
    private final VmClassLoaderImpl classLoader;

    // combination vtable, itable, constructor table, and static method table
    private final Map<ExecutableElement, VmInvokable> methodTable = new ConcurrentHashMap<>();

    // object layout

    /**
     * This is the layout of instances of this class.
     */
    private final LayoutInfo layoutInfo;
    /**
     * This is the singleton layout for the static fields of this class.
     */
    private final LayoutInfo staticLayoutInfo;

    // memory

    /**
     * This is the memory which backs the static fields of this class, as defined by {@link #staticLayoutInfo}.
     */
    private final Memory staticMemory;

    private volatile List<? extends VmClassImpl> interfaces;
    private volatile VmClassImpl superClass;
    private volatile VmArrayClassImpl arrayClass;

    // initialization state

    private volatile State state = State.UNINITIALIZED;
    private volatile VmThrowableImpl initException;
    private final Object initLock = new Object();

    VmClassImpl(VmImpl vmImpl, LoadedTypeDefinition typeDefinition) {
        this(vmImpl, vmImpl.classClass, typeDefinition);
    }

    /**
     * Construct a normal `Class` instance.
     *
     * @param vmImpl the VM (must not be {@code null})
     * @param classClass the `Class.class` instance (must not be {@code null})
     * @param typeDefinition the type definition of the class being defined (must not be {@code null})
     */
    VmClassImpl(VmImpl vmImpl, VmClassClassImpl classClass, LoadedTypeDefinition typeDefinition) {
        super(classClass);
        vm = vmImpl;
        this.typeDefinition = typeDefinition;
        ClassContext classContext = typeDefinition.getContext();
        classLoader = (VmClassLoaderImpl) classContext.getClassLoader();
        CompilationContext ctxt = classContext.getCompilationContext();
        layoutInfo = typeDefinition.isInterface() ? null : Layout.get(ctxt).getInstanceLayoutInfo(typeDefinition);
        staticLayoutInfo = Layout.get(ctxt).getStaticLayoutInfo(typeDefinition);
        staticMemory = staticLayoutInfo == null ? MemoryFactory.getEmpty() : vmImpl.allocate(staticLayoutInfo.getStructType(), 1);
        initializeConstantStaticFields();
    }

    VmClassImpl(VmImpl vmImpl, VmClassClassImpl classClass, LoadedTypeDefinition typeDefinition, @SuppressWarnings("unused") int primitivesOnly) {
        // special ctor for primitive classes
        super(classClass);
        vm = vmImpl;
        state = State.INITIALIZED;
        this.typeDefinition = typeDefinition;
        typeDefinition.setVmClass(this);
        classLoader = null;
        layoutInfo = null;
        staticLayoutInfo = null;
        staticMemory = vmImpl.emptyMemory;
        interfaces = List.of();
    }

    VmClassImpl(final VmImpl vm, final ClassContext classContext, @SuppressWarnings("unused") Class<VmClassClassImpl> classClassOnly) {
        // special ctor for Class.class, where getClass() == Class.class
        super(vm, VmClassImpl.class, Layout.get(classContext.getCompilationContext()).getInstanceLayoutInfo(classContext.findDefinedType("java/lang/Class").load()));
        this.vm = vm;
        typeDefinition = classContext.findDefinedType("java/lang/Class").load();
        typeDefinition.setVmClass(this);
        classLoader = null;
        CompilationContext ctxt = classContext.getCompilationContext();
        layoutInfo = Layout.get(ctxt).getInstanceLayoutInfo(typeDefinition);
        staticLayoutInfo = Layout.get(ctxt).getStaticLayoutInfo(typeDefinition);
        staticMemory = staticLayoutInfo == null ? MemoryFactory.getEmpty() : vm.allocate(staticLayoutInfo.getStructType(), 1);
        superClass = new VmClassImpl(vm, (VmClassClassImpl) this, classContext.findDefinedType("java/lang/Object").load());
        initializeConstantStaticFields();
    }

    Pointer computeBitMap() {
        CompilationContext ctxt = vm.getCompilationContext();
        TypeSystem ts = ctxt.getTypeSystem();
        PointerType bitMapType = ts.getUnsignedInteger32Type().getPointer().asWide();
        LayoutInfo layoutInfo = this.layoutInfo;
        if (layoutInfo == null) {
            // no layout; primitive type or other special case
            return new IntegerAsPointer(bitMapType, 0);
        }
        StructType st = layoutInfo.getStructType();
        try {
            return new IntegerAsPointer(bitMapType, computeSmallBitMap(ts, st, 0L, 0));
        } catch (BigBitMapException e) {
            // do the more complex job for big bitmaps
            int refSize = ts.getReferenceSize();
            long size = st.getSize();
            int ec = (int) ((size + refSize - 1) / refSize);
            int[] bitMap = new int[ec];
            computeBigBitMap(ts, st, bitMap, 0);
            // define the data
            LiteralFactory lf = ctxt.getLiteralFactory();
            List<Literal> values = new ArrayList<>(bitMap.length);
            for (int val : bitMap) {
                values.add(lf.literalOf(val));
            }
            ArrayType arrayType = ts.getArrayType(bitMapType.getPointeeType(), bitMap.length);
            Data data = ctxt.getImplicitSection(typeDefinition).addData(null, "bitMap", lf.literalOf(arrayType, values));
            data.setLinkage(Linkage.PRIVATE);
            return data.getPointer();
        }
    }

    String getPackageInternalName() {
        String internalName = getTypeDefinition().getInternalName();
        int idx = internalName.lastIndexOf('/');
        if (idx == -1) {
            return "";
        } else {
            return internalName.substring(0, idx);
        }
    }

    @SuppressWarnings("serial")
    static final class BigBitMapException extends RuntimeException {
        private static final StackTraceElement[] EMPTY_STACK = new StackTraceElement[0];

        BigBitMapException() {
            setStackTrace(EMPTY_STACK);
        }
    }

    long computeSmallBitMap(TypeSystem ts, ArrayType at, long bitMap, int base) {
        // treat array[0] as a flexible array; set 1x the bits for it
        long ec = Math.max(at.getElementCount(), 1);
        long es = at.getElementSize();
        ValueType elementType = at.getElementType();
        for (long i = 0; i < ec; i ++) {
            bitMap |= computeSmallBitMap(ts, elementType, bitMap, (int) (base + i * es));
        }
        return bitMap;
    }

    void computeBigBitMap(TypeSystem ts, ArrayType at, int[] bitMap, int base) {
        // treat array[0] as a flexible array; set 1x the bits for it
        long ec = Math.max(at.getElementCount(), 1);
        long es = at.getElementSize();
        ValueType elementType = at.getElementType();
        for (long i = 0; i < ec; i ++) {
            computeBigBitMap(ts, elementType, bitMap, (int) (base + i * es));
        }
    }

    long computeSmallBitMap(TypeSystem ts, StructType st, long bitMap, int base) {
        for (StructType.Member member : st.getMembers()) {
            bitMap |= computeSmallBitMap(ts, member.getType(), bitMap, base + member.getOffset());
        }
        return bitMap;
    }

    void computeBigBitMap(TypeSystem ts, StructType st, int[] bitMap, int base) {
        for (StructType.Member member : st.getMembers()) {
            computeBigBitMap(ts, member.getType(), bitMap, base + member.getOffset());
        }
    }

    long computeSmallBitMap(TypeSystem ts, ValueType itemType, long bitMap, int offset) {
        if (itemType instanceof StructType nct) {
            bitMap |= computeSmallBitMap(ts, nct, bitMap, offset);
        } else if (itemType instanceof ArrayType at) {
            bitMap |= computeSmallBitMap(ts, at, bitMap, offset);
        } else if (itemType instanceof ReferenceType) {
            int refSize = ts.getReferenceSize();
            int refShift = Integer.numberOfTrailingZeros(refSize);
            int idx = offset >> refShift;
            if (idx >= 64) {
                throw new BigBitMapException();
            }
            bitMap |= 1L << idx;
        }
        return bitMap;
    }

    void computeBigBitMap(TypeSystem ts, ValueType itemType, int[] bitMap, int offset) {
        if (itemType instanceof StructType nct) {
            computeBigBitMap(ts, nct, bitMap, offset);
        } else if (itemType instanceof ArrayType at) {
            computeBigBitMap(ts, at, bitMap, offset);
        } else if (itemType instanceof ReferenceType) {
            int refSize = ts.getReferenceSize();
            int refShift = Integer.numberOfTrailingZeros(refSize);
            int idx = offset >> refShift;
            if (idx >= 64) {
                throw new BigBitMapException();
            }
            bitMap[idx >> 5] |= 1 << idx;
        }
    }

    void initializeConstantStaticFields() {
        int cnt = typeDefinition.getFieldCount();
        for (int i = 0; i < cnt; i ++) {
            FieldElement field = typeDefinition.getField(i);
            if (field instanceof StaticFieldElement sfe) try {
                Literal initValue = sfe.getInitialValue();
                if (initValue == null || initValue instanceof ZeroInitializerLiteral) {
                    // Nothing to do;  memory starts zeroed.
                    continue;
                }
                StructType.Member member = staticLayoutInfo.getMember(field);
                if (initValue instanceof IntegerLiteral val) {
                    if (field.getType().getSize() == 1) {
                        staticMemory.store8(member.getOffset(), val.byteValue(), SinglePlain);
                    } else if (field.getType().getSize() == 2) {
                        staticMemory.store16(member.getOffset(), val.shortValue(), SinglePlain);
                    } else if (field.getType().getSize() == 4) {
                        staticMemory.store32(member.getOffset(), val.intValue(), SinglePlain);
                    } else {
                        staticMemory.store64(member.getOffset(), val.longValue(), SinglePlain);
                    }
                } else if (initValue instanceof FloatLiteral val) {
                    if (field.getType().getSize() == 4) {
                        staticMemory.storeFloat(member.getOffset(), val.floatValue(), SinglePlain);
                    } else {
                        staticMemory.storeDouble(member.getOffset(), val.doubleValue(), SinglePlain);
                    }
                } else if (initValue instanceof StringLiteral) {
                    if (vm.bootstrapComplete) {
                        VmString sv = vm.intern(((StringLiteral) initValue).getValue());
                        staticMemory.storeRef(member.getOffset(), sv,SinglePlain);
                    }
                } else {
                    // CONSTANT_Class, CONSTANT_MethodHandle, CONSTANT_MethodType
                    vm.getCompilationContext().warning("Did not properly initialize interpreter memory for constant static field "+field);
                }
            } catch (IndexOutOfBoundsException e) {
                throw e; // breakpoint
            }
        }
    }

    void setComponentClass(VmClass componentClass) {
        memory.storeRef(indexOf(clazz.typeDefinition.findField("componentType")), componentClass, SingleRelease);
    }

    @Override
    public VmArrayClassImpl getArrayClass() {
        VmArrayClassImpl arrayClazz = this.arrayClass;
        if (arrayClazz == null) {
            synchronized (this) {
                arrayClazz = this.arrayClass;
                if (arrayClazz == null) {
                    arrayClazz = this.arrayClass = constructArrayClass();
                }
                arrayClazz.setComponentClass(this);
                memory.storeRef(indexOf(CoreClasses.get(vm.getCompilationContext()).getArrayClassField()), arrayClazz, SingleRelease);
            }
        }
        return arrayClazz;
    }

    @Override
    public VmClassClassImpl getVmClass() {
        return (VmClassClassImpl) super.getVmClass();
    }

    static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    void postConstruct(VmImpl vm) {
        String name = typeDefinition.getInternalName().replace('/', '.');
        if (typeDefinition.isHidden()) {
            name += "/" + ENCODER.encodeToString(typeDefinition.getDigest()) + '.' + typeDefinition.getHiddenClassIndex();
        }
        postConstruct(name, vm);
    }

    void postConstruct(final String name, VmImpl vm) {
        // todo: Base JDK equivalent core classes with appropriate manual initializer
        CoreClasses coreClasses = CoreClasses.get(vm.getCompilationContext());
        LoadedTypeDefinition jlcDef = getVmClass().getTypeDefinition();
        try {
            memory.storeRef(getVmClass().getLayoutInfo().getMember(jlcDef.findField("name")).getOffset(), vm.intern(name), SinglePlain);
            memory.storeRef(getVmClass().getLayoutInfo().getMember(jlcDef.findField("classLoader")).getOffset(), classLoader, SinglePlain);
            VmObject rd = vm.manuallyInitialize(vm.reflectionDataClass.newInstance());
            memory.storeRef(getVmClass().getLayoutInfo().getMember(jlcDef.findField("qbiccReflectionData")).getOffset(), rd, SinglePlain);

            // typeId and dimensions
            FieldElement instanceTypeIdField = coreClasses.getClassTypeIdField();
            if (this instanceof  VmPrimitiveClassImpl pci) {
                memory.storeType(indexOf(instanceTypeIdField), pci.getPrimitive().getType(), SinglePlain);
                memory.store32(getVmClass().getLayoutInfo().getMember(coreClasses.getClassInstanceSizeField()).getOffset(), pci.getPrimitive().getType().getSize(), SinglePlain);
                memory.store8(getVmClass().getLayoutInfo().getMember(coreClasses.getClassInstanceAlignField()).getOffset(), pci.getPrimitive().getType().getAlign(), SinglePlain);
            } else if (this instanceof VmReferenceArrayClass vmArray) {
                memory.storeType(indexOf(instanceTypeIdField), vmArray.getInstanceObjectType().getLeafElementType(), SinglePlain);
                memory.store8(indexOf(coreClasses.getClassDimensionField()), vmArray.getInstanceObjectType().getDimensionCount(), SinglePlain);
            } else {
                memory.storeType(indexOf(instanceTypeIdField), this.getInstanceObjectTypeId(), SinglePlain);
            }

            if (layoutInfo != null) {
                memory.store32(getVmClass().getLayoutInfo().getMember(coreClasses.getClassInstanceSizeField()).getOffset(), layoutInfo.getStructType().getSize(), SinglePlain);
                memory.store8(getVmClass().getLayoutInfo().getMember(coreClasses.getClassInstanceAlignField()).getOffset(), layoutInfo.getStructType().getAlign(), SinglePlain);
            }
            memory.store32(getVmClass().getLayoutInfo().getMember(jlcDef.findField("modifiers")).getOffset(), typeDefinition.getModifiers(), SinglePlain);
            setPointerField(jlcDef, "referenceBitMap", computeBitMap());
            if (!typeDefinition.isPrimitive() && !(this instanceof VmArrayClassImpl)) {
                String sig = typeDefinition.getSignature().toString();
                if (!sig.isEmpty()) {
                    setRefField(jlcDef, "genericSignature", vm.intern(sig));
                }
            }
        } catch (Exception e) {
            // for breakpoints
            throw e;
        }
        vm.manuallyInitialize(this);
    }

    void initVmClass() {
        typeDefinition.setVmClass(this);
    }

    VmArrayClassImpl constructArrayClass() {
        // assume reference array by default
        LoadedTypeDefinition arrayDef = CoreClasses.get(typeDefinition.getContext().getCompilationContext()).getRefArrayContentField().getEnclosingType().load();
        VmRefArrayClassImpl clazz = new VmRefArrayClassImpl(getVm(), getVmClass(), arrayDef, this);
        VmClassImpl jlcClass = clazz.clazz;
        Memory memory = clazz.getMemory();
        memory.storeRef(jlcClass.layoutInfo.getMember(jlcClass.typeDefinition.findField("componentType")).getOffset(), this, SinglePlain);
        clazz.postConstruct(getVm());
        return clazz;
    }

    VmImpl getVm() {
        return vm;
    }

    @Override
    public VmObject getLookupObject(int allowedModes) {
        VmClassImpl lookupClass = vm.getBootstrapClassLoader().loadClass("java/lang/invoke/MethodHandles$Lookup");
        LoadedTypeDefinition lookupDef = lookupClass.getTypeDefinition();
        VmObjectImpl lookup = vm.manuallyInitialize(lookupClass.newInstance());
        lookup.getMemory().storeRef(lookup.indexOf(lookupDef.findField("lookupClass")), this, SinglePlain);
        lookup.getMemory().store32(lookup.indexOf(lookupDef.findField("allowedModes")), allowedModes, SinglePlain);
        VarHandle.releaseFence();
        return lookup;
    }

    @Override
    public String getName() {
        return ((VmString)memory.loadRef(indexOf(getVmClass().getTypeDefinition().findField("name")), SinglePlain)).getContent();
    }

    @Override
    public String getSimpleName() {
        return getName();
    }

    @Override
    public ObjectType getInstanceObjectType() {
        return typeDefinition.getObjectType();
    }

    @Override
    public ObjectType getInstanceObjectTypeId() {
        return getInstanceObjectType();
    }

    @Override
    public LoadedTypeDefinition getTypeDefinition() {
        LoadedTypeDefinition typeDefinition = this.typeDefinition;
        if (typeDefinition == null) {
            throw new IllegalStateException("No type definition for this type");
        }
        return typeDefinition;
    }

    @Override
    public VmClassImpl getSuperClass() {
        VmClassImpl superClass = this.superClass;
        if (superClass == null) {
            LoadedTypeDefinition typeDefinition = this.typeDefinition;
            if (typeDefinition != null) {
                LoadedTypeDefinition def = typeDefinition.getSuperClass();
                if (def == null) {
                    // no superclass
                    return null;
                } else {
                    superClass = (VmClassImpl) def.getVmClass();
                }
                this.superClass = superClass;
            }
        }
        return superClass;
    }

    @Override
    public List<? extends VmClass> getInterfaces() {
        List<? extends VmClassImpl> interfaces = this.interfaces;
        if (interfaces == null) {
            List<? extends VmClassImpl> newVal;
            LoadedTypeDefinition typeDefinition = this.typeDefinition;
            if (typeDefinition == null || typeDefinition.getInterfaceCount() == 0) {
                // no interfaces
                newVal = List.of();
            } else {
                VmClassImpl[] array = new VmClassImpl[typeDefinition.getInterfaceCount()];
                int i = 0;
                for (LoadedTypeDefinition def : typeDefinition.getInterfaces()) {
                    // load each interface
                    array[i] = (VmClassImpl) def.getVmClass();
                }
                newVal = List.of(array);
            }
            do {
                if (interfacesHandle.compareAndSet(this, null, newVal)) {
                    return newVal;
                }
                interfaces = this.interfaces;
            } while (interfaces == null);
        }
        return interfaces;
    }

    @Override
    public VmClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public Memory getStaticMemory() {
        return staticMemory;
    }

    LayoutInfo getLayoutInfo() {
        return layoutInfo;
    }

    public Literal getValueForStaticField(FieldElement field) {
        int offset = staticLayoutInfo.getMember(field).getOffset();
        ValueType valueType = field.getType();
        return getLiteral(offset, valueType, staticMemory);
    }

    private Literal getLiteral(final int offset, final ValueType valueType, final Memory memory) {
        LiteralFactory lf = vm.getCompilationContext().getLiteralFactory();
        if (valueType instanceof BooleanType) {
            int val = memory.load8(offset, SinglePlain);
            return lf.literalOf(val != 0);
        } else if (valueType instanceof SignedIntegerType sit) {
            return switch (sit.getMinBits()) {
                case 8 -> lf.literalOf((byte) memory.load8(offset, SinglePlain));
                case 16 -> lf.literalOf((short) memory.load16(offset, SinglePlain));
                case 32 -> lf.literalOf(memory.load32(offset, SinglePlain));
                case 64 -> {
                    try {
                        yield lf.literalOf(memory.load64(offset, SinglePlain));
                    } catch (InvalidMemoryAccessException e) {
                        Pointer value = memory.loadPointer(offset, SinglePlain);
                        if (value == null) {
                            yield lf.zeroInitializerLiteralOfType(sit);
                        } else {
                            yield lf.valueConvertLiteral(lf.literalOf(value), sit);
                        }
                    }
                }
                default -> throw Assert.impossibleSwitchCase(sit.getMinBits());
            };
        } else if (valueType instanceof UnsignedIntegerType uit) {
            return switch (uit.getMinBits()) {
                case 8 -> lf.literalOf(uit, memory.load8(offset, SinglePlain));
                case 16 -> lf.literalOf((char) memory.load16(offset, SinglePlain));
                case 32 -> lf.literalOf(uit, memory.load32(offset, SinglePlain));
                case 64 -> lf.literalOf(uit, memory.load64(offset, SinglePlain));
                default -> throw Assert.impossibleSwitchCase(uit.getMinBits());
            };
        } else if (valueType instanceof FloatType ft) {
            return switch (ft.getMinBits()) {
                case 32 -> lf.literalOf(memory.loadFloat(offset, SinglePlain));
                case 64 -> lf.literalOf(memory.loadDouble(offset, SinglePlain));
                default -> throw Assert.impossibleSwitchCase(ft.getMinBits());
            };
        } else if (valueType instanceof ReferenceType rt) {
            VmObject value = memory.loadRef(offset, SinglePlain);
            if (value == null) {
                return lf.nullLiteralOfType(rt);
            } else {
                return lf.literalOf(value);
            }
        } else if (valueType instanceof PointerType pt) {
            Pointer value = memory.loadPointer(offset, SinglePlain);
            if (value == null) {
                return lf.nullLiteralOfType(pt);
            } else {
                return lf.literalOf(value);
            }
        } else if (valueType instanceof ArrayType at) {
            final long elementCount = at.getElementCount();
            if (elementCount > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Array too large");
            }
            final ValueType elementType = at.getElementType();
            if (elementType instanceof IntegerType it) {
                if (it.getMinBits() == 8) {
                    // make a byte array
                    final byte[] array = new byte[(int) elementCount];
                    for (int i = 0; i < array.length; i++) {
                        array[i] = (byte) memory.load8(offset + i, SinglePlain);
                    }
                    return lf.literalOf(at, array);
                } else if (it.getMinBits() == 16) {
                    // make a short array
                    final short[] array = new short[(int) elementCount];
                    for (int i = 0; i < array.length; i++) {
                        array[i] = (short) memory.load16(offset + ((long) i << 1), SinglePlain);
                    }
                    return lf.literalOf(at, array);
                }
            }
            final long elementSize = at.getElementSize();
            Literal[] literals = new Literal[(int) elementCount];
            for (int i = 0; i < elementCount; i++) {
                literals[i] = getLiteral(offset + (i * (int) elementSize), elementType, memory);
            }
            return lf.literalOf(at, List.of(literals));
        } else if (valueType instanceof StructType st) {
            Map<StructType.Member, Literal> map = new HashMap<>(st.getMemberCount());
            for (StructType.Member member : st.getMembers()) {
                map.put(member, getLiteral(offset + member.getOffset(), member.getType(), memory));
            }
            return lf.literalOf(st, map);
        } else {
            throw new IllegalStateException("Unsupported type");
        }
    }

    @Override
    public int indexOfStatic(FieldElement field) throws IllegalArgumentException {
        LoadedTypeDefinition loaded = field.getEnclosingType().load();
        CompilationContext ctxt = loaded.getContext().getCompilationContext();
        LayoutInfo layoutInfo = Layout.get(ctxt).getStaticLayoutInfo(loaded);
        if (layoutInfo != null) {
            StructType.Member member = layoutInfo.getMember(field);
            if (member != null) {
                return member.getOffset();
            }
        }
        throw new IllegalArgumentException("Field " + field + " is not present on " + this);
    }

    void initialize(VmThreadImpl thread) throws Thrown {
        VmClassImpl superClass = getSuperClass();
        if (superClass != null) {
            superClass.initialize(thread);
        }
        State state = this.state;
        VmThrowableImpl initException = this.initException; // always written before state
        if (state == State.UNINITIALIZED || state == State.INITIALIZING) {
            synchronized (initLock) {
                state = this.state;
                initException = this.initException; // always written before state
                if (state == State.UNINITIALIZED) {
                    log.debugf("Initializing %s", getName());
                    this.state = State.INITIALIZING;
                    try {
                        InitializerElement initializer = typeDefinition.getInitializer();
                        if (initializer != null && initializer.hasMethodBodyFactory()) {
                            if (initializer.tryCreateMethodBody()) {
                                // set up context class loader
                                final VmClassLoader old = thread.getContextClassLoader();
                                thread.setContextClassLoader(getClassLoader());
                                try {
                                    compile(initializer).invoke(thread, null, List.of());
                                } finally {
                                    // restore previous loader (in case of e.g. recursive init)
                                    thread.setContextClassLoader(old);
                                }
                                state = this.state = State.INITIALIZED;
                            } else {
                                throw new Thrown(vm.linkageErrorClass.newInstance("Failed to compile initializer body for " + getName()));
                            }
                        } else {
                            state = this.state = State.INITIALIZED;
                        }
                        log.debugf("Initialization completed for %s", getName());
                    } catch (Thrown t) {
                        initException = this.initException = (VmThrowableImpl) t.getThrowable();
                        state = this.state = State.INITIALIZATION_FAILED;
                        log.debugf(t, "Failed to initialize %s", getName());
                    } catch (Throwable t) {
                        vm.getCompilationContext().error(t, "Crash in interpreter while initializing %s: %s", this, t);
                        state = this.state = State.INITIALIZATION_FAILED;
                        log.debugf(t, "Failed to initialize %s", getName());
                    }
                }
            }
        }
        if (state == State.INITIALIZATION_FAILED) {
            VmImpl vm = thread.getVM();
            ClassContext bcc = vm.getCompilationContext().getBootstrapClassContext();
            LoadedTypeDefinition errorType = bcc.findDefinedType("java/lang/ExceptionInInitializerError").load();
            ClassObjectType exType = errorType.getClassType();
            VmThrowableClassImpl clazz = (VmThrowableClassImpl) errorType.getVmClass();
            VmThrowable obj;
            if (initException != null) {
                obj = clazz.newInstance(initException);
            } else {
                obj = clazz.newInstance("Failed to initialize " + getName() + " (no nested exception)");
            }
            throw new Thrown(obj);
        }
    }

    VmInvokable getOrCompile(ExecutableElement element) {
        VmInvokable target = methodTable.get(element);
        if (target == null) {
            if (element.tryCreateMethodBody()) {
                target = compile(element);
            } else {
                target = (thread, instance, args) -> {
                    // treat it like an unsatisfied link
                    VmThrowableClassImpl uleClazz = (VmThrowableClassImpl) vm.getBootstrapClassLoader().loadClass("java/lang/UnsatisfiedLinkError");
                    String name;
                    if (element instanceof MethodElement) {
                        name = ((MethodElement) element).getName();
                    } else {
                        throw new IllegalStateException("Unknown native element");
                    }
                    throw new Thrown(uleClazz.newInstance(getName() + "." + name));
                };
            }
            VmInvokable appearing = methodTable.putIfAbsent(element, target);
            if (appearing != null) {
                target = appearing;
            }
        }
        return target;
    }

    public void registerInvokable(final ExecutableElement element, final VmInvokable invokable) {
        Assert.checkNotNullParam("invokable", invokable);
        synchronized (methodTable) {
            if (methodTable.putIfAbsent(element, invokable) != null) {
                throw new IllegalStateException("Attempted to register an invokable for an already-compiled method");
            }
        }
    }

    void registerInvokable(final String methodName, final VmInvokable invokable) {
        Assert.checkNotNullParam("invokable", invokable);
        LoadedTypeDefinition td = getTypeDefinition();
        int idx = td.findSingleMethodIndex(me -> me.nameEquals(methodName));
        if (idx == -1) {
            throw new IllegalArgumentException("No method named " + methodName + " found on " + this);
        }
        registerInvokable(td.getMethod(idx), invokable);
    }

    void registerInvokable(final String methodName, final int nArgs, final VmInvokable invokable) {
        Assert.checkNotNullParam("invokable", invokable);
        LoadedTypeDefinition td = getTypeDefinition();
        int idx = td.findSingleMethodIndex(me -> me.nameEquals(methodName) && me.getParameters().size() == nArgs);
        if (idx == -1) {
            throw new IllegalArgumentException("No method named " + methodName + " with " + nArgs + " argument(s) found on " + this);
        }
        registerInvokable(td.getMethod(idx), invokable);
    }

    private VmInvokable compile(ExecutableElement element) {
        if (! element.tryCreateMethodBody()) {
            return (thread, target, args) -> {
                throw new Thrown(vm.linkageErrorClass.newInstance("No method body"));
            };
        }
        return new VmInvokableImpl(element);
    }

    /**
     * Construct a new instance of the type corresponding to this {@code VmClass}.  Implementations may return
     * a more specific object instance class.
     *
     * @return the new instance (not {@code null})
     */
    VmObjectImpl newInstance() {
        return new VmObjectImpl(this);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    @Override
    StringBuilder toString(StringBuilder target) {
        target.append(typeDefinition.isInterface() ? "interface" : "class");
        return target.append(' ').append(getName());
    }

    boolean shouldBeInitialized() {
        return state == State.UNINITIALIZED;
    }

    public TypeDescriptor getDescriptor() {
        return typeDefinition.getDescriptor();
    }

    @Override
    public boolean isAssignableFrom(VmClass other) {
        if (other instanceof VmPrimitiveClass) {
            return false;
        }
        ObjectType ourType = getInstanceObjectType();
        ObjectType otherType = other.getInstanceObjectType();
        return ourType.isSupertypeOf(otherType);
    }

    void addNestMember(final VmClassImpl member) {
        CoreClasses coreClasses = CoreClasses.get(vm.getCompilationContext());
        final int nestMembersIdx = indexOf(coreClasses.getClassNestMembersField());
        VmRefArrayImpl oldMembers, newMembers, witness;
        int insert;
        oldMembers = (VmRefArrayImpl) getMemory().loadRef(nestMembersIdx, SingleAcquire);
        for (;;) {
            if (oldMembers == null) {
                insert = 1;
                newMembers = (VmRefArrayImpl) vm.newArrayOf(getVmClass(), 2);
                newMembers.store(0, this);
            } else {
                insert = oldMembers.getLength();
                newMembers = (VmRefArrayImpl) vm.newArrayOf(getVmClass(), insert + 1);
                System.arraycopy(oldMembers.getArray(), 0, newMembers.getArray(), 0, insert);
            }
            newMembers.store(insert, member);
            witness = (VmRefArrayImpl) getMemory().compareAndExchangeRef(nestMembersIdx, oldMembers, newMembers, SingleAcquire, SingleRelease);
            if (witness == oldMembers) {
                // got it
                return;
            }
            oldMembers = witness;
        }
    }

    void setNestHost(final VmClassImpl host) {
        // this is a `final` field
        CoreClasses coreClasses = CoreClasses.get(vm.getCompilationContext());
        getMemory().storeRef(indexOf(coreClasses.getClassNestHostField()), host, SingleRelease);
    }

    void setModule(final VmObjectImpl module) {
        CoreClasses coreClasses = CoreClasses.get(vm.getCompilationContext());
        getMemory().storeRef(indexOf(coreClasses.getClassModuleField()), module, SingleRelease);
    }

    VmClassImpl getNestHost() {
        CoreClasses coreClasses = CoreClasses.get(vm.getCompilationContext());
        VmClassImpl nestHost = (VmClassImpl) getMemory().loadRef(indexOf(coreClasses.getClassNestHostField()), SingleAcquire);
        // todo: temporary workaround
        if (nestHost == null) {
            LoadedTypeDefinition typeDefinition = this.typeDefinition;
            if (typeDefinition == null) {
                setNestHost(this);
                return this;
            } else {
                DefinedTypeDefinition nestHostDef = typeDefinition.getNestHost();
                if (nestHostDef == null) {
                    setNestHost(this);
                    return this;
                } else {
                    nestHost = ((VmClassImpl) nestHostDef.load().getVmClass()).getNestHost();
                    setNestHost(nestHost);
                    nestHost.addNestMember(this);
                }
            }
        }
        return nestHost;
    }

    enum State {
        UNINITIALIZED,
        INITIALIZING,
        INITIALIZATION_FAILED,
        INITIALIZED,
        ;
    }
}

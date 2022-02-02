package org.qbicc.interpreter.impl;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
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
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
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
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

import static org.qbicc.graph.atomic.AccessModes.*;

class VmClassImpl extends VmObjectImpl implements VmClass {
    private static final Logger log = Logger.getLogger("org.qbicc.interpreter");

    private static final VarHandle interfacesHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "interfaces", VarHandle.class, VmClassImpl.class, List.class);

    private final VmImpl vm;
    /**
     * This is the type definition corresponding to the class represented by this instance.
     */
    private final LoadedTypeDefinition typeDefinition;
    private final VmClassLoaderImpl classLoader;
    private final VmObject protectionDomain;

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
    private final VmStaticFieldBase staticFieldBase;

    // memory

    /**
     * This is the memory which backs the static fields of this class, as defined by {@link #staticLayoutInfo}.
     */
    private final MemoryImpl staticMemory;

    private volatile List<? extends VmClassImpl> interfaces;
    private volatile VmClassImpl superClass;
    private volatile VmArrayClassImpl arrayClass;

    // initialization state

    private volatile State state = State.UNINITIALIZED;
    private volatile VmThrowableImpl initException;
    private final Object initLock = new Object();

    VmClassImpl(VmImpl vmImpl, LoadedTypeDefinition typeDefinition, VmObject protectionDomain) {
        this(vmImpl, vmImpl.classClass, typeDefinition, protectionDomain);
    }

    /**
     * Construct a normal `Class` instance.
     *
     * @param vmImpl the VM (must not be {@code null})
     * @param classClass the `Class.class` instance (must not be {@code null})
     * @param typeDefinition the type definition of the class being defined (must not be {@code null})
     * @param protectionDomain the protection domain
     */
    VmClassImpl(VmImpl vmImpl, VmClassClassImpl classClass, LoadedTypeDefinition typeDefinition, VmObject protectionDomain) {
        super(classClass);
        vm = vmImpl;
        this.typeDefinition = typeDefinition;
        this.protectionDomain = protectionDomain;
        ClassContext classContext = typeDefinition.getContext();
        classLoader = (VmClassLoaderImpl) classContext.getClassLoader();
        CompilationContext ctxt = classContext.getCompilationContext();
        layoutInfo = typeDefinition.isInterface() ? null : Layout.get(ctxt).getInstanceLayoutInfo(typeDefinition);
        staticLayoutInfo = Layout.get(ctxt).getStaticLayoutInfo(typeDefinition);
        staticMemory = staticLayoutInfo == null ? vmImpl.allocate(0) : vmImpl.allocate((int) staticLayoutInfo.getCompoundType().getSize());
        initializeConstantStaticFields();
        staticFieldBase = staticLayoutInfo == null ? null : new VmStaticFieldBase(staticLayoutInfo, staticMemory);
    }

    VmClassImpl(VmImpl vmImpl, VmClassClassImpl classClass, @SuppressWarnings("unused") int primitivesOnly) {
        // special ctor for primitive classes
        super(classClass);
        vm = vmImpl;
        state = State.INITIALIZED;
        typeDefinition = null;
        protectionDomain = null;
        classLoader = null;
        layoutInfo = null;
        staticLayoutInfo = null;
        staticMemory = vmImpl.emptyMemory;
        interfaces = List.of();
        staticFieldBase = null;
    }

    VmClassImpl(final VmImpl vm, final ClassContext classContext, @SuppressWarnings("unused") Class<VmClassClassImpl> classClassOnly) {
        // special ctor for Class.class, where getClass() == Class.class
        super(vm, VmClassImpl.class, Layout.get(classContext.getCompilationContext()).getInstanceLayoutInfo(classContext.findDefinedType("java/lang/Class").load()));
        this.vm = vm;
        typeDefinition = classContext.findDefinedType("java/lang/Class").load();
        protectionDomain = null;
        classLoader = null;
        CompilationContext ctxt = classContext.getCompilationContext();
        layoutInfo = Layout.get(ctxt).getInstanceLayoutInfo(typeDefinition);
        staticLayoutInfo = Layout.get(ctxt).getStaticLayoutInfo(typeDefinition);
        staticMemory = staticLayoutInfo == null ? vm.allocate(0) : vm.allocate((int) staticLayoutInfo.getCompoundType().getSize());
        superClass = new VmClassImpl(vm, (VmClassClassImpl) this, classContext.findDefinedType("java/lang/Object").load(), null);
        initializeConstantStaticFields();
        staticFieldBase = staticLayoutInfo == null ? null : new VmStaticFieldBase(staticLayoutInfo, staticMemory);
    }

    void initializeConstantStaticFields() {
        int cnt = typeDefinition.getFieldCount();
        for (int i = 0; i < cnt; i ++) {
            FieldElement field = typeDefinition.getField(i);
            if (field.isStatic()) try {
                Literal initValue = field.getInitialValue();
                if (initValue == null || initValue instanceof ZeroInitializerLiteral) {
                    // Nothing to do;  memory starts zeroed.
                    continue;
                }
                CompoundType.Member member = staticLayoutInfo.getMember(field);
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
                        staticMemory.store32(member.getOffset(), val.floatValue(), SinglePlain);
                    } else {
                        staticMemory.store64(member.getOffset(), val.doubleValue(), SinglePlain);
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

    void postConstruct(VmImpl vm) {
        String name = typeDefinition.getInternalName().replace('/', '.');
        if (typeDefinition.hasAllModifiersOf(ClassFile.I_ACC_HIDDEN)) {
            VmClassLoaderImpl realClassLoader = vm.getClassLoaderForContext(typeDefinition.getContext());
            name += "/" + realClassLoader.getHiddenClassSeq(name);
        }
        postConstruct(name, vm);
    }

    void postConstruct(final String name, VmImpl vm) {
        // todo: Base JDK equivalent core classes with appropriate manual initializer
        CoreClasses coreClasses = CoreClasses.get(vm.getCompilationContext());
        try {
            memory.storeRef(getVmClass().getLayoutInfo().getMember(getVmClass().getTypeDefinition().findField("name")).getOffset(), vm.intern(name), SinglePlain);

            // typeId and dimensions
            FieldElement instanceTypeIdField = coreClasses.getClassTypeIdField();
            if (this instanceof  VmPrimitiveClassImpl pci) {
                memory.storeType(indexOf(instanceTypeIdField), pci.getPrimitive().getType(), SinglePlain);
            } else if (this instanceof VmReferenceArrayClass vmArray) {
                memory.storeType(indexOf(instanceTypeIdField), vmArray.getInstanceObjectType().getLeafElementType(), SinglePlain);
                memory.store8(indexOf(coreClasses.getClassDimensionField()), vmArray.getInstanceObjectType().getDimensionCount(), SinglePlain);
            } else {
                memory.storeType(indexOf(instanceTypeIdField), this.getInstanceObjectTypeId(), SinglePlain);
            }

            if (layoutInfo != null) {
                memory.store32(getVmClass().getLayoutInfo().getMember(coreClasses.getClassInstanceSizeField()).getOffset(), layoutInfo.getCompoundType().getSize(), SinglePlain);
                memory.store8(getVmClass().getLayoutInfo().getMember(coreClasses.getClassInstanceAlignField()).getOffset(), layoutInfo.getCompoundType().getAlign(), SinglePlain);
            }
        } catch (Exception e) {
            // for breakpoints
            throw e;
        }
        vm.manuallyInitialize(this);
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

    public VmObject getProtectionDomain() {
        return protectionDomain;
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
    public VmObject getStaticFieldBase() {
        return staticFieldBase;
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
        return typeDefinition.getType();
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
                    VmClassLoader classLoader = getVm().getClassLoaderForContext(def.getContext());
                    superClass = (VmClassImpl) classLoader.loadClass(def.getInternalName());
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
                    VmClassLoader classLoader = getVm().getClassLoaderForContext(def.getContext());
                    array[i] = (VmClassImpl) classLoader.loadClass(def.getInternalName());
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
    public MemoryImpl getStaticMemory() {
        return staticMemory;
    }

    LayoutInfo getLayoutInfo() {
        return layoutInfo;
    }

    public Literal getValueForStaticField(FieldElement field) {
        int offset = staticLayoutInfo.getMember(field).getOffset();
        TypeDescriptor desc = field.getTypeDescriptor();
        if (desc.equals(BaseTypeDescriptor.Z)) {
            int val = staticMemory.load8(offset, SinglePlain);
            return vm.getCompilationContext().getLiteralFactory().literalOf(val != 0);
        } else if (desc.equals(BaseTypeDescriptor.B)) {
            return vm.getCompilationContext().getLiteralFactory().literalOf((byte) staticMemory.load8(offset, SinglePlain));
        } else if (desc.equals(BaseTypeDescriptor.S)) {
            return vm.getCompilationContext().getLiteralFactory().literalOf((short) staticMemory.load16(offset, SinglePlain));
        } else if (desc.equals(BaseTypeDescriptor.C)) {
            return vm.getCompilationContext().getLiteralFactory().literalOf((char) staticMemory.load16(offset, SinglePlain));
        } else if (desc.equals(BaseTypeDescriptor.I)) {
            return vm.getCompilationContext().getLiteralFactory().literalOf(staticMemory.load32(offset, SinglePlain));
        } else if (desc.equals(BaseTypeDescriptor.F)) {
            return vm.getCompilationContext().getLiteralFactory().literalOf(staticMemory.loadFloat(offset, SinglePlain));
        } else if (desc.equals(BaseTypeDescriptor.J)) {
            return vm.getCompilationContext().getLiteralFactory().literalOf(staticMemory.load64(offset, SinglePlain));
        } else if (desc.equals(BaseTypeDescriptor.D)) {
            return vm.getCompilationContext().getLiteralFactory().literalOf(staticMemory.loadDouble(offset, SinglePlain));
        } else {
            VmObject value = staticMemory.loadRef(offset, SinglePlain);
            if (value == null) {
                return vm.getCompilationContext().getLiteralFactory().zeroInitializerLiteralOfType(field.getType());
            } else {
                return vm.getCompilationContext().getLiteralFactory().literalOf(value);
            }
        }
    }

    @Override
    public int indexOfStatic(FieldElement field) throws IllegalArgumentException {
        LoadedTypeDefinition loaded = field.getEnclosingType().load();
        CompilationContext ctxt = loaded.getContext().getCompilationContext();
        LayoutInfo layoutInfo = Layout.get(ctxt).getStaticLayoutInfo(loaded);
        if (layoutInfo != null) {
            CompoundType.Member member = layoutInfo.getMember(field);
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
                    this.state = State.INITIALIZING;
                    try {
                        InitializerElement initializer = typeDefinition.getInitializer();
                        if (initializer != null && initializer.hasMethodBodyFactory()) {
                            if (initializer.tryCreateMethodBody()) {
                                compile(initializer).invoke(thread, null, List.of());
                                state = this.state = State.INITIALIZED;
                            } else {
                                throw new Thrown(vm.linkageErrorClass.newInstance("Failed to compile initializer body for " + getName()));
                            }
                        } else {
                            state = this.state = State.INITIALIZED;
                        }
                    } catch (Thrown t) {
                        initException = this.initException = (VmThrowableImpl) t.getThrowable();
                        state = this.state = State.INITIALIZATION_FAILED;
                        log.debug("Failed to initialize a class", t);
                    } catch (Throwable t) {
                        vm.getCompilationContext().error(t, "Crash in interpreter while initializing %s", this);
                        state = this.state = State.INITIALIZATION_FAILED;
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
            thread.setThrown(obj);
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

    @Override
    public void registerInvokable(final ExecutableElement element, final VmInvokable invokable) {
        Assert.checkNotNullParam("invokable", invokable);
        synchronized (methodTable) {
            if (methodTable.putIfAbsent(element, invokable) != null) {
                throw new IllegalStateException("Attempted to register an invokable for an already-compiled method");
            }
        }
    }

    @Override
    public void registerInvokable(String name, MethodDescriptor descriptor, VmInvokable invokable) throws IllegalStateException {
        if (name.equals("<clinit>")) {
            if (descriptor.equals(MethodDescriptor.VOID_METHOD_DESCRIPTOR)) {
                registerInvokable(typeDefinition.getInitializer(), invokable);
            } else {
                throw new IllegalArgumentException("Invalid initializer descriptor " + descriptor + " on " + this);
            }
        } else if (name.equals("<init>")) {
            int idx = typeDefinition.findConstructorIndex(descriptor);
            if (idx == -1) {
                throw new IllegalArgumentException("No constructor found with descriptor " + descriptor + " on " + this);
            }
            registerInvokable(typeDefinition.getConstructor(idx), invokable);
        } else {
            int idx = typeDefinition.findMethodIndex(name, descriptor);
            if (idx == -1) {
                throw new IllegalArgumentException("No method found with name \"" + name + "\" and descriptor " + descriptor + " on " + this);
            }
            registerInvokable(typeDefinition.getMethod(idx), invokable);
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
                insert = 0;
                newMembers = (VmRefArrayImpl) vm.newArrayOf(getVmClass(), 1);
            } else {
                insert = oldMembers.getLength();
                newMembers = (VmRefArrayImpl) vm.newArrayOf(getVmClass(), insert + 1);
                for (int i = 0; i < insert; i ++) {
                    newMembers.store(i, oldMembers.getMemory().loadRef(oldMembers.getArrayElementOffset(i), SinglePlain));
                }
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

    enum State {
        UNINITIALIZED,
        INITIALIZING,
        INITIALIZATION_FAILED,
        INITIALIZED,
        ;
    }
}

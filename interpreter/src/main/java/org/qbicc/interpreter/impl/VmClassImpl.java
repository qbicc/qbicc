package org.qbicc.interpreter.impl;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThrowable;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

class VmClassImpl extends VmObjectImpl implements VmClass {
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
    private final Layout.LayoutInfo layoutInfo;
    /**
     * This is the singleton layout for the static fields of this class.
     */
    private final Layout.LayoutInfo staticLayoutInfo;

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
        layoutInfo = typeDefinition.isInterface() ? null : Layout.getForInterpreter(ctxt).getInstanceLayoutInfo(typeDefinition);
        staticLayoutInfo = Layout.getForInterpreter(ctxt).getInterpreterStaticLayoutInfo(typeDefinition);
        staticMemory = staticLayoutInfo == null ? vmImpl.allocate(0) : vmImpl.allocate((int) staticLayoutInfo.getCompoundType().getSize());
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
    }

    VmClassImpl(final VmImpl vm, final ClassContext classContext, @SuppressWarnings("unused") Class<VmClassClassImpl> classClassOnly) {
        // special ctor for Class.class, where getClass() == Class.class
        super(vm, VmClassImpl.class, Layout.getForInterpreter(classContext.getCompilationContext()).getInstanceLayoutInfo(classContext.findDefinedType("java/lang/Class").load()));
        this.vm = vm;
        typeDefinition = classContext.findDefinedType("java/lang/Class").load();
        protectionDomain = null;
        classLoader = null;
        CompilationContext ctxt = classContext.getCompilationContext();
        layoutInfo = Layout.getForInterpreter(ctxt).getInstanceLayoutInfo(typeDefinition);
        staticLayoutInfo = Layout.getForInterpreter(ctxt).getInterpreterStaticLayoutInfo(typeDefinition);
        staticMemory = staticLayoutInfo == null ? vm.allocate(0) : vm.allocate((int) staticLayoutInfo.getCompoundType().getSize());
        superClass = new VmClassImpl(vm, (VmClassClassImpl) this, classContext.findDefinedType("java/lang/Object").load(), null);
    }

    VmArrayClassImpl getArrayClass() {
        VmArrayClassImpl arrayClazz = this.arrayClass;
        if (arrayClazz == null) {
            synchronized (this) {
                arrayClazz = this.arrayClass;
                if (arrayClazz == null) {
                    arrayClazz = this.arrayClass = constructArrayClass();
                }
            }
        }
        return arrayClazz;
    }

    @Override
    public VmClassClassImpl getVmClass() {
        return (VmClassClassImpl) super.getVmClass();
    }

    void setName(VmImpl vm) {
        setName(typeDefinition.getInternalName().replace('/', '.'), vm);
    }

    void setName(final String name, VmImpl vm) {
        try {
            memory.storeRef(getVmClass().getLayoutInfo().getMember(getVmClass().getTypeDefinition().findField("name")).getOffset(), vm.intern(name), MemoryAtomicityMode.UNORDERED);
        } catch (Exception e) {
            // for breakpoints
            throw e;
        }
    }

    VmArrayClassImpl constructArrayClass() {
        // assume reference array by default
        LoadedTypeDefinition arrayDef = CoreClasses.get(typeDefinition.getContext().getCompilationContext()).getRefArrayContentField().getEnclosingType().load();
        return new VmRefArrayClassImpl(getVm(), getVmClass(), arrayDef, this);
    }

    VmImpl getVm() {
        return vm;
    }

    public VmObject getProtectionDomain() {
        return protectionDomain;
    }

    @Override
    public String getName() {
        return typeDefinition.getInternalName();
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
    public LoadedTypeDefinition getTypeDefinition() {
        LoadedTypeDefinition typeDefinition = this.typeDefinition;
        if (typeDefinition == null) {
            throw new IllegalStateException("No type definition for this type");
        }
        return typeDefinition;
    }

    @Override
    public VmClassImpl getSuperClass() {
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

    MemoryImpl getStaticMemory() {
        return staticMemory;
    }

    Layout.LayoutInfo getLayoutInfo() {
        return layoutInfo;
    }

    void initialize(VmThreadImpl thread) throws Thrown {
        VmClassImpl superClass = this.superClass;
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
                        if (initializer.hasMethodBodyFactory()) {
                            if (initializer.tryCreateMethodBody()) {
                                compile(initializer).invoke(thread, null, List.of());
                                state = this.state = State.INITIALIZED;
                            } else {
                                throw new IllegalStateException("Failed to compile initializer body");
                            }
                        }
                    } catch (Thrown t) {
                        initException = this.initException = (VmThrowableImpl) t.getThrowable();
                        state = this.state = State.INITIALIZATION_FAILED;
                    } catch (Throwable t) {
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
            VmThrowable obj = (VmThrowable) vm.allocateObject(exType);
            if (initException != null) {
                MethodDescriptor causeMethodDesc = MethodDescriptor.synthesize(bcc, BaseTypeDescriptor.V, List.of(ClassTypeDescriptor.synthesize(bcc, "java/lang/Throwable")));
                vm.invokeExact(errorType.resolveConstructorElement(causeMethodDesc), obj, List.of(initException));
            } else {
                vm.invokeExact(errorType.resolveConstructorElement(MethodDescriptor.VOID_METHOD_DESCRIPTOR), obj, List.of());
            }
            throw new Thrown(obj);
        }
    }

    VmInvokable getOrCompile(ExecutableElement element) {
        VmInvokable target = methodTable.get(element);
        if (target == null) {
            // only compile one at a time to avoid redundant work
            synchronized (methodTable) {
                target = methodTable.get(element);
                if (target == null) {
                    methodTable.put(element, target = compile(element));
                }
            }
        }
        return target;
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

    enum State {
        UNINITIALIZED,
        INITIALIZING,
        INITIALIZATION_FAILED,
        INITIALIZED,
        ;
    }
}

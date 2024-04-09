package org.qbicc.interpreter.impl;

import static java.lang.invoke.MethodHandles.lookup;
import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ByteArrayLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.MethodHandleLiteral;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeIdLiteral;
import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.Signal;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmArrayClass;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmInvokable;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmPrimitiveClass;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThread;
import org.qbicc.interpreter.memory.MemoryFactory;
import org.qbicc.plugin.apploader.AppClassLoader;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.pointer.IntegerAsPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.PointerType;
import org.qbicc.type.Primitive;
import org.qbicc.type.UnresolvedType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.AnnotatedElement;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.methodhandle.ConstructorMethodHandleConstant;
import org.qbicc.type.methodhandle.FieldMethodHandleConstant;
import org.qbicc.type.methodhandle.MethodHandleConstant;
import org.qbicc.type.methodhandle.MethodMethodHandleConstant;

public final class VmImpl implements Vm {
    private final CompilationContext ctxt;
    private final Map<GlobalVariableElement, Memory> globals = new ConcurrentHashMap<>();
    private final Map<String, VmStringImpl> interned = new ConcurrentHashMap<>();
    final VmClassLoaderImpl bootstrapClassLoader;
    private final AtomicBoolean initialized = new AtomicBoolean();
    private final Consumer<VmObject> manualInitializers;
    private List<String> propertyDefines;

    final MemoryImpl emptyMemory;

    final MethodHandle toPointerConversion;

    boolean bootstrapComplete;

    // core classes
    final VmClassImpl objectClass;
    final VmClassClassImpl classClass;
    final VmClassImpl reflectionDataClass;
    final VmStringClassImpl stringClass;
    final int stringCoderOffset;
    final int stringValueOffset;
    final VmThreadClassImpl threadClass;
    final VmThrowableClassImpl throwableClass;
    final VmClassLoaderClassImpl classLoaderClass;

    final MethodElement toStringMethod;

    // primitive classes
    final VmPrimitiveClassImpl byteClass;
    final VmPrimitiveClassImpl shortClass;
    final VmPrimitiveClassImpl intClass;
    final VmPrimitiveClassImpl longClass;
    final VmPrimitiveClassImpl floatClass;
    final VmPrimitiveClassImpl doubleClass;
    final VmPrimitiveClassImpl charClass;
    final VmPrimitiveClassImpl booleanClass;
    final VmPrimitiveClassImpl voidClass;

    // array classes
    final int arrayLengthOffset;

    final int byteArrayContentOffset;
    final VmByteArrayClassImpl byteArrayClass;
    final int shortArrayContentOffset;
    final VmShortArrayClassImpl shortArrayClass;
    final int intArrayContentOffset;
    final VmIntArrayClassImpl intArrayClass;
    final int longArrayContentOffset;
    final VmLongArrayClassImpl longArrayClass;
    final int floatArrayContentOffset;
    final VmFloatArrayClassImpl floatArrayClass;
    final int doubleArrayContentOffset;
    final VmDoubleArrayClassImpl doubleArrayClass;
    final int charArrayContentOffset;
    final VmCharArrayClassImpl charArrayClass;
    final int booleanArrayContentOffset;
    final VmBooleanArrayClassImpl booleanArrayClass;

    final int refArrayContentOffset; // special

    // exceptions
    final VmThrowableClassImpl interruptedException;
    final VmThrowableClassImpl illegalMonitorStateException;

    final VmThrowableClassImpl runtimeException;

    final VmThrowableClassImpl nullPointerException;

    // error classes
    final VmThrowableClassImpl errorClass;

    final VmThrowableClassImpl linkageErrorClass;

    final VmThrowableClassImpl incompatibleClassChangeErrorClass;
    final VmThrowableClassImpl noClassDefFoundErrorClass;
    final VmThrowableClassImpl classNotFoundExceptionClass;
    final VmThrowableClassImpl verifyErrorClass;

    final VmThrowableClassImpl noSuchFieldErrorClass;
    final VmThrowableClassImpl noSuchMethodErrorClass;

    final VmClassImpl stackTraceElementClass;

    // regular classes
    volatile VmClassImpl propertiesClass;

    volatile MethodElement setPropertyMethod;

    volatile VmObject mainThreadGroup;

    final Set<VmThreadImpl> startedThreads = ConcurrentHashMap.newKeySet();

    VmImpl(final CompilationContext ctxt, Consumer<VmObject> manualInitializers) {
        this.ctxt = ctxt;
        toPointerConversion = toPointerConversionRaw.bindTo(ctxt.getTypeSystem().getVoidType().getPointer());
        this.manualInitializers = manualInitializers;
        bootstrapComplete = false;
        // force all fields to be populated so the injections are visible to us
        CoreClasses coreClasses = CoreClasses.get(ctxt);
        boolean be = ctxt.getTypeSystem().getEndianness() == ByteOrder.BIG_ENDIAN;
        emptyMemory = be ? BigEndianMemoryImpl.EMPTY : LittleEndianMemoryImpl.EMPTY;
        ClassContext bcc = ctxt.getBootstrapClassContext();
        classClass = new VmClassClassImpl(this);
        objectClass = classClass.getSuperClass();
        reflectionDataClass = new VmClassImpl(this, bcc.findDefinedType("java/lang/Class$ReflectionData").load());
        classLoaderClass = new VmClassLoaderClassImpl(this, bcc.findDefinedType("java/lang/ClassLoader").load());
        LoadedTypeDefinition stringDef = bcc.findDefinedType("java/lang/String").load();
        stringClass = new VmStringClassImpl(this, stringDef);
        FieldElement coderField = stringDef.findField("coder");
        FieldElement valueField = stringDef.findField("value");
        Layout layout = Layout.get(ctxt);
        LayoutInfo stringLayout = layout.getInstanceLayoutInfo(stringDef);
        stringCoderOffset = stringLayout.getMember(coderField).getOffset();
        stringValueOffset = stringLayout.getMember(valueField).getOffset();
        toStringMethod = objectClass.getTypeDefinition().resolveMethodElementExact(bcc, "toString", MethodDescriptor.synthesize(bcc, stringDef.getDescriptor(), List.of()));
        threadClass = new VmThreadClassImpl(this, bcc.findDefinedType("java/lang/Thread").load());
        throwableClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/Throwable").load());

        byteClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getByteTypeDefinition(), coreClasses.getByteArrayTypeDefinition());
        shortClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getShortTypeDefinition(), coreClasses.getShortArrayTypeDefinition());
        intClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getIntTypeDefinition(), coreClasses.getIntArrayTypeDefinition());
        longClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getLongTypeDefinition(), coreClasses.getLongArrayTypeDefinition());
        floatClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getFloatTypeDefinition(), coreClasses.getFloatArrayTypeDefinition());
        doubleClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getDoubleTypeDefinition(), coreClasses.getDoubleArrayTypeDefinition());
        charClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getCharTypeDefinition(), coreClasses.getCharArrayTypeDefinition());
        booleanClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getBooleanTypeDefinition(), coreClasses.getBooleanArrayTypeDefinition());
        voidClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getVoidTypeDefinition(), null);

        FieldElement arrayLengthField = coreClasses.getArrayLengthField();
        LoadedTypeDefinition arrayBaseClassDef = arrayLengthField.getEnclosingType().load();
        arrayLengthOffset = layout.getInstanceLayoutInfo(arrayBaseClassDef).getMember(arrayLengthField).getOffset();

        FieldElement byteArrayContentField = coreClasses.getByteArrayContentField();
        LoadedTypeDefinition byteArrayClassDef = byteArrayContentField.getEnclosingType().load();
        byteArrayClass = new VmByteArrayClassImpl(this, classClass, byteArrayClassDef, byteClass);
        byteArrayContentOffset = layout.getInstanceLayoutInfo(byteArrayClassDef).getMember(byteArrayContentField).getOffset();

        FieldElement shortArrayContentField = coreClasses.getShortArrayContentField();
        LoadedTypeDefinition shortArrayClassDef = shortArrayContentField.getEnclosingType().load();
        shortArrayClass = new VmShortArrayClassImpl(this, classClass, shortArrayClassDef, shortClass);
        shortArrayContentOffset = layout.getInstanceLayoutInfo(shortArrayClassDef).getMember(shortArrayContentField).getOffset();

        FieldElement intArrayContentField = coreClasses.getIntArrayContentField();
        LoadedTypeDefinition intArrayClassDef = intArrayContentField.getEnclosingType().load();
        intArrayClass = new VmIntArrayClassImpl(this, classClass, intArrayClassDef, intClass);
        intArrayContentOffset = layout.getInstanceLayoutInfo(intArrayClassDef).getMember(intArrayContentField).getOffset();

        FieldElement longArrayContentField = coreClasses.getLongArrayContentField();
        LoadedTypeDefinition longArrayClassDef = longArrayContentField.getEnclosingType().load();
        longArrayClass = new VmLongArrayClassImpl(this, classClass, longArrayClassDef, longClass);
        longArrayContentOffset = layout.getInstanceLayoutInfo(longArrayClassDef).getMember(longArrayContentField).getOffset();

        FieldElement floatArrayContentField = coreClasses.getFloatArrayContentField();
        LoadedTypeDefinition floatArrayClassDef = floatArrayContentField.getEnclosingType().load();
        floatArrayClass = new VmFloatArrayClassImpl(this, classClass, floatArrayClassDef, floatClass);
        floatArrayContentOffset = layout.getInstanceLayoutInfo(floatArrayClassDef).getMember(floatArrayContentField).getOffset();

        FieldElement doubleArrayContentField = coreClasses.getDoubleArrayContentField();
        LoadedTypeDefinition doubleArrayClassDef = doubleArrayContentField.getEnclosingType().load();
        doubleArrayClass = new VmDoubleArrayClassImpl(this, classClass, doubleArrayClassDef, doubleClass);
        doubleArrayContentOffset = layout.getInstanceLayoutInfo(doubleArrayClassDef).getMember(doubleArrayContentField).getOffset();

        FieldElement charArrayContentField = coreClasses.getCharArrayContentField();
        LoadedTypeDefinition charArrayClassDef = charArrayContentField.getEnclosingType().load();
        charArrayClass = new VmCharArrayClassImpl(this, classClass, charArrayClassDef, charClass);
        charArrayContentOffset = layout.getInstanceLayoutInfo(charArrayClassDef).getMember(charArrayContentField).getOffset();

        FieldElement booleanArrayContentField = coreClasses.getBooleanArrayContentField();
        LoadedTypeDefinition booleanArrayClassDef = booleanArrayContentField.getEnclosingType().load();
        booleanArrayClass = new VmBooleanArrayClassImpl(this, classClass, booleanArrayClassDef, booleanClass);
        booleanArrayContentOffset = layout.getInstanceLayoutInfo(booleanArrayClassDef).getMember(booleanArrayContentField).getOffset();

        refArrayContentOffset = layout.getInstanceLayoutInfo(coreClasses.getReferenceArrayTypeDefinition()).getMember(coreClasses.getRefArrayContentField()).getOffset();

        classClass.postConstruct(this);
        reflectionDataClass.postConstruct(this);
        objectClass.postConstruct(this);
        classLoaderClass.postConstruct(this);
        stringClass.postConstruct(this);
        threadClass.postConstruct(this);
        throwableClass.postConstruct(this);
        byteClass.postConstruct(this);
        shortClass.postConstruct(this);
        intClass.postConstruct(this);
        longClass.postConstruct(this);
        floatClass.postConstruct(this);
        doubleClass.postConstruct(this);
        charClass.postConstruct(this);
        booleanClass.postConstruct(this);
        voidClass.postConstruct(this);

        byteArrayClass.postConstruct(this);
        shortArrayClass.postConstruct(this);
        intArrayClass.postConstruct(this);
        longArrayClass.postConstruct(this);
        floatArrayClass.postConstruct(this);
        doubleArrayClass.postConstruct(this);
        charArrayClass.postConstruct(this);
        booleanArrayClass.postConstruct(this);

        byteClass.setArrayClass(ctxt, byteArrayClass);
        shortClass.setArrayClass(ctxt, shortArrayClass);
        intClass.setArrayClass(ctxt, intArrayClass);
        longClass.setArrayClass(ctxt, longArrayClass);
        floatClass.setArrayClass(ctxt, floatArrayClass);
        doubleClass.setArrayClass(ctxt, doubleArrayClass);
        charClass.setArrayClass(ctxt, charArrayClass);
        booleanClass.setArrayClass(ctxt, booleanArrayClass);

        byteArrayClass.setComponentClass(byteClass);
        shortArrayClass.setComponentClass(shortClass);
        intArrayClass.setComponentClass(intClass);
        longArrayClass.setComponentClass(longClass);
        floatArrayClass.setComponentClass(floatClass);
        doubleArrayClass.setComponentClass(doubleClass);
        charArrayClass.setComponentClass(charClass);
        booleanArrayClass.setComponentClass(booleanClass);

        // throwables
        errorClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/Error").load());
        errorClass.postConstruct(this);

        // exceptions
        interruptedException = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/InterruptedException").load());
        interruptedException.postConstruct(this);
        illegalMonitorStateException = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/IllegalMonitorStateException").load());
        illegalMonitorStateException.postConstruct(this);

        runtimeException = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/RuntimeException").load());
        runtimeException.postConstruct(this);
        nullPointerException = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/NullPointerException").load());
        nullPointerException.postConstruct(this);

        // errors
        linkageErrorClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/LinkageError").load());
        linkageErrorClass.postConstruct(this);

        // linkage errors
        incompatibleClassChangeErrorClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/IncompatibleClassChangeError").load());
        incompatibleClassChangeErrorClass.postConstruct(this);
        noClassDefFoundErrorClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/NoClassDefFoundError").load());
        noClassDefFoundErrorClass.postConstruct(this);
        classNotFoundExceptionClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/ClassNotFoundException").load());
        classNotFoundExceptionClass.postConstruct(this);
        verifyErrorClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/VerifyError").load());
        verifyErrorClass.postConstruct(this);

        // incompatible class change errors
        noSuchMethodErrorClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/NoSuchMethodError").load());
        noSuchMethodErrorClass.postConstruct(this);
        noSuchFieldErrorClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/NoSuchFieldError").load());
        noSuchFieldErrorClass.postConstruct(this);

        stackTraceElementClass = new VmClassImpl(this, bcc.findDefinedType("java/lang/StackTraceElement").load());
        stackTraceElementClass.postConstruct(this);

        // set up the bootstrap class loader *last*
        bootstrapClassLoader = new VmClassLoaderImpl(classLoaderClass, this);

        // now register the classes
        bootstrapClassLoader.registerClass("java/lang/Object", objectClass);
        bootstrapClassLoader.registerClass("java/lang/Class", classClass);
        bootstrapClassLoader.registerClass("java/lang/String", stringClass);
        bootstrapClassLoader.registerClass("java/lang/Thread", threadClass);
        bootstrapClassLoader.registerClass("java/lang/Throwable", throwableClass);
        bootstrapClassLoader.registerClass("java/lang/ClassLoader", classLoaderClass);

        bootstrapClassLoader.registerClass("java/lang/InterruptedException", interruptedException);
        bootstrapClassLoader.registerClass("java/lang/IllegalMonitorStateException", illegalMonitorStateException);
        bootstrapClassLoader.registerClass("java/lang/RuntimeException", runtimeException);
        bootstrapClassLoader.registerClass("java/lang/NullPointerException", nullPointerException);
        bootstrapClassLoader.registerClass("java/lang/Error", errorClass);
        bootstrapClassLoader.registerClass("java/lang/LinkageError", linkageErrorClass);
        bootstrapClassLoader.registerClass("java/lang/IncompatibleClassChangeError", incompatibleClassChangeErrorClass);
        bootstrapClassLoader.registerClass("java/lang/NoClassDefFoundError", noClassDefFoundErrorClass);
        bootstrapClassLoader.registerClass("java/lang/NoSuchMethodError", noSuchMethodErrorClass);

        bootstrapClassLoader.registerClass("java/lang/StackTraceElement", stackTraceElementClass);

        bootstrapClassLoader.registerClass("[B", byteArrayClass);
        bootstrapClassLoader.registerClass("[S", shortArrayClass);
        bootstrapClassLoader.registerClass("[I", intArrayClass);
        bootstrapClassLoader.registerClass("[J", longArrayClass);
        bootstrapClassLoader.registerClass("[F", floatArrayClass);
        bootstrapClassLoader.registerClass("[D", doubleArrayClass);
        bootstrapClassLoader.registerClass("[C", charArrayClass);
        bootstrapClassLoader.registerClass("[Z", booleanArrayClass);

        throwableClass.initializeConstantStaticFields(); // Has constant String fields that can't be initialized when we first process the class

    }

    VmClassLoaderImpl getBootstrapClassLoader() {
        return bootstrapClassLoader;
    }

    public CompilationContext getCompilationContext() {
        return ctxt;
    }

    @Override
    public void initialize() {
        VmThreadImpl vmThread = (VmThreadImpl) Vm.requireCurrentThread();
        if (initialized.compareAndSet(false, true)) {

            propertiesClass = bootstrapClassLoader.loadClass("java/util/Properties");

            LoadedTypeDefinition propertiesTypeDef = propertiesClass.getTypeDefinition();
            int idx = propertiesTypeDef.findSingleMethodIndex(me -> me.nameEquals("setProperty"));
            if (idx == -1) {
                throw new IllegalStateException("Missing required method in VM");
            }
            setPropertyMethod = propertiesTypeDef.getMethod(idx);

            // Create System ThreadGroup and set the initializing Thread's group to be it
            mainThreadGroup = createMainThreadGroup();
            vmThread.setThreadGroup(mainThreadGroup);

            // Register all hooks
            VmClassLoaderImpl bootstrapClassLoader = this.bootstrapClassLoader;

            // Object
            registerHooks(bootstrapClassLoader.loadClass("java/lang/Object"), HooksForObject.class, lookup());
            // LockSupport
            registerHooks(bootstrapClassLoader.loadClass("java/util/concurrent/locks/LockSupport"), HooksForLockSupport.class, lookup());
            // VMHelpers
            registerHooks(bootstrapClassLoader.loadClass("org/qbicc/runtime/main/VMHelpers"), HooksForVMHelpers.class, lookup());
            // CompilerIntrinsics
            registerHooks(bootstrapClassLoader.loadClass("org/qbicc/runtime/main/CompilerIntrinsics"), HooksForCompilerIntrinsics.class, lookup());
            // Unsafe
            registerHooks(bootstrapClassLoader.loadClass("jdk/internal/misc/Unsafe"), HooksForUnsafe.class, lookup());
            // System (we use the class object later)
            final VmClassImpl systemClass = bootstrapClassLoader.loadClass("java/lang/System");
            registerHooks(systemClass, HooksForSystem.class, lookup());
            // Runtime
            registerHooks(bootstrapClassLoader.loadClass("java/lang/Runtime"), HooksForRuntime.class, lookup());
            // SystemProps$Raw
            registerHooks(bootstrapClassLoader.loadClass("jdk/internal/util/SystemProps$Raw"), HooksForSystemPropsRaw.class, lookup());
            // StackTraceElement
            registerHooks(bootstrapClassLoader.loadClass("java/lang/StackTraceElement"), HooksForStackTraceElement.class, lookup());
            // StrictMath
            registerHooks(bootstrapClassLoader.loadClass("java/lang/StrictMath"), HooksForStrictMath.class, lookup());
            // String (we use the class object later)
            VmClassImpl stringClass = bootstrapClassLoader.loadClass("java/lang/String");
            registerHooks(stringClass, HooksForString.class, lookup());
            // Thread
            registerHooks(bootstrapClassLoader.loadClass("java/lang/Thread"), HooksForThread.class, lookup());
            // Throwable (we use the class object later)
            VmClassImpl throwableClass = bootstrapClassLoader.loadClass("java/lang/Throwable");
            registerHooks(throwableClass, HooksForThrowable.class, lookup());
            // Class (we use the class object later)
            VmClassImpl classClass = bootstrapClassLoader.loadClass("java/lang/Class");
            registerHooks(classClass, HooksForClass.class, lookup());
            // ClassLoader
            registerHooks(bootstrapClassLoader.loadClass("java/lang/ClassLoader"), HooksForClassLoader.class, lookup());
            // BuiltinClassLoader
            registerHooks(bootstrapClassLoader.loadClass("jdk/internal/loader/BuiltinClassLoader"), HooksForBuiltinClassLoader.class, lookup());
            // ServiceLoader
            registerHooks(bootstrapClassLoader.loadClass("java/util/QbiccServiceLoaderSupport"), HooksForQbiccServiceLoaderSupport.class, lookup());
            // Module
            registerHooks(bootstrapClassLoader.loadClass("java/lang/Module"), HooksForModule.class, lookup());
            // Array
            registerHooks(bootstrapClassLoader.loadClass("java/lang/reflect/Array"), HooksForArray.class, lookup());
            // Reflection
            registerHooks(bootstrapClassLoader.loadClass("jdk/internal/reflect/Reflection"), HooksForReflection.class, lookup());
            // OSEnvironment
            registerHooks(bootstrapClassLoader.loadClass("jdk/internal/misc/OSEnvironment"), HooksForOSEnvironment.class, lookup());
            // UnixNativeDispatcher
            registerHooks(bootstrapClassLoader.loadClass("sun/nio/fs/UnixNativeDispatcher"), HooksForUnixNativeDispatcher.class, lookup());
            // FileDescriptor
            registerHooks(bootstrapClassLoader.loadClass("java/io/FileDescriptor"), HooksForFileDescriptor.class, lookup());
            // ProcessEnvironment
            registerHooks(bootstrapClassLoader.loadClass("java/lang/ProcessEnvironment"), HooksForProcessEnvironment.class, lookup());
            // Build
            registerHooks(bootstrapClassLoader.loadClass("org/qbicc/runtime/Build"), HooksForBuild.class, lookup());
            // CRC32
            registerHooks(bootstrapClassLoader.loadClass("java/util/zip/CRC32"), HooksForCRC32.class, lookup());
            // SeedGenerator
            registerHooks(bootstrapClassLoader.loadClass("sun/security/provider/SeedGenerator"), HooksForSeedGenerator.class, lookup());
            // VerifyAccess
            registerHooks(bootstrapClassLoader.loadClass("sun/invoke/util/VerifyAccess"), HooksForVerifyAccess.class, lookup());
            // MethodHandles$Lookup
            registerHooks(bootstrapClassLoader.loadClass("java/lang/invoke/MethodHandles$Lookup"), HooksForMethodHandlesLookup.class, lookup());
            // Instant
            registerHooks(bootstrapClassLoader.loadClass("java/time/Instant"), HooksForInstant.class, lookup());
            // Bits
            registerHooks(bootstrapClassLoader.loadClass("java/nio/Bits"), HooksForBits.class, lookup());

            // Now execute system initialization
            LoadedTypeDefinition systemType = systemClass.getTypeDefinition();
            // phase 1
            invokeExact(systemType.getMethod(systemType.findSingleMethodIndex(me -> me.nameEquals("initPhase1"))), null, List.of());
        }
    }

    public void initialize2() {
        VmThreadImpl vmThread = (VmThreadImpl) Vm.requireCurrentThread();
        VmClassLoaderImpl bootstrapClassLoader = this.bootstrapClassLoader;

        VmClassImpl systemClass = bootstrapClassLoader.loadClass("java/lang/System");
        LoadedTypeDefinition systemType = systemClass.getTypeDefinition();

        // phase 2
        invokeExact(systemType.getMethod(systemType.findSingleMethodIndex(me -> me.nameEquals("initPhase2"))), null, List.of(Boolean.FALSE, Boolean.FALSE));
        // phase 3
        invokeExact(systemType.getMethod(systemType.findSingleMethodIndex(me -> me.nameEquals("initPhase3"))), null, List.of());

        // Initialize early to avoid deadlocks
        initialize(bootstrapClassLoader.loadClass("java/lang/ref/Reference"));
        initialize(bootstrapClassLoader.loadClass("java/util/concurrent/ForkJoinPool"));
    }

    // Convert '.' to '/' matching the functionality of fixClassname in the JDK's check_classname.c
    VmString fixClassname(VmString name) {
        String n = name.getContent();
        if (n.indexOf('.') == -1) {
            return name;
        }
        return intern(n.replace('.', '/'));
    }

    public VmThread newThread(final String threadName, final VmObject threadGroup, final boolean daemon, int priority) {
        VmThreadImpl vmThread = new VmThreadImpl(threadClass, this);
        manuallyInitialize(vmThread);
        if (threadGroup != null) {
            vmThread.setThreadGroup(threadGroup);
        }
        return vmThread;
    }

    public DefinedTypeDefinition loadClass(ClassContext classContext, final String name) throws Thrown {
        VmClass loaded = getClassLoaderForContext(classContext).loadClass(name);
        return loaded == null ? null : loaded.getTypeDefinition();
    }

    public byte[] loadResource(final ClassContext classContext, final String name) throws Thrown {
        VmThread currentThread = Vm.requireCurrentThread();
        VmClassLoaderImpl classLoader = getClassLoaderForContext(classContext);
        // our invokers
        MethodElement getResourceAsStream = classLoaderClass.getTypeDefinition().requireSingleMethod("getResourceAsStream");
        MethodElement readAllBytes = bootstrapClassLoader.loadClass("java/io/InputStream").getTypeDefinition().requireSingleMethod("readAllBytes");
        MethodElement close = bootstrapClassLoader.loadClass("java/io/InputStream").getTypeDefinition().requireSingleMethod("close");
        VmObject stream = (VmObject) getInstanceInvoker(getResourceAsStream).invokeAny(currentThread, classLoader, List.of(intern(name)));
        if (stream == null) {
            return null;
        }
        VmByteArrayImpl array;
        try {
            array = (VmByteArrayImpl) getInstanceInvoker(readAllBytes).invokeAny(currentThread, stream, List.of());
        } catch (Throwable t) {
            getInstanceInvoker(close).invokeAny(currentThread, stream, List.of());
            throw t;
        }
        getInstanceInvoker(close).invokeAny(currentThread, stream, List.of());
        return array.getArray();
    }

    public List<byte[]> loadResources(final ClassContext classContext, final String name) throws Thrown {
        VmThread currentThread = Vm.requireCurrentThread();
        VmClassLoaderImpl classLoader = getClassLoaderForContext(classContext);
        // this is particularly ugly
        MethodElement getResources = classLoaderClass.getTypeDefinition().requireSingleMethod("getResources");
        VmClassImpl enumerationClass = bootstrapClassLoader.loadClass("java/util/Enumeration");
        MethodElement hasMoreElements = enumerationClass.getTypeDefinition().requireSingleMethod("hasMoreElements");
        MethodElement nextElement = enumerationClass.getTypeDefinition().requireSingleMethod("nextElement");
        VmClassImpl urlClass = bootstrapClassLoader.loadClass("java/net/URL");
        MethodElement openStream = urlClass.getTypeDefinition().requireSingleMethod("openStream");
        MethodElement readAllBytes = bootstrapClassLoader.loadClass("java/io/InputStream").getTypeDefinition().requireSingleMethod("readAllBytes");
        MethodElement close = bootstrapClassLoader.loadClass("java/io/InputStream").getTypeDefinition().requireSingleMethod("close");
        VmObject enumeration = (VmObject) getVirtualInvoker(getResources, classLoader).invokeAny(currentThread, classLoader, List.of(intern(name)));
        ArrayList<byte[]> list = new ArrayList<>();
        while (((Boolean) getVirtualInvoker(hasMoreElements, enumeration).invokeAny(currentThread, enumeration, List.of())).booleanValue()) {
            VmObject url = (VmObject) getVirtualInvoker(nextElement, enumeration).invokeAny(currentThread, enumeration, List.of());
            // open the stream
            VmObject stream = (VmObject) getVirtualInvoker(openStream, url).invokeAny(currentThread, url, List.of());
            if (stream != null) {
                VmByteArrayImpl array;
                try {
                    array = (VmByteArrayImpl) getVirtualInvoker(readAllBytes, stream).invokeAny(currentThread, stream, List.of());
                } catch (Throwable t) {
                    getVirtualInvoker(close, stream).invokeAny(currentThread, stream, List.of());
                    throw t;
                }
                getInstanceInvoker(close).invokeAny(currentThread, stream, List.of());
                list.add(array.getArray());
            }
        }
        return List.copyOf(list);
    }

    public VmObject allocateObject(final ClassObjectType type) {
        DefinedTypeDefinition def = type.getDefinition();
        ClassContext context = def.getContext();
        VmClassLoaderImpl classLoader = (VmClassLoaderImpl) context.getClassLoader();
        if (classLoader == null) {
            classLoader = getBootstrapClassLoader();
        }
        VmClassImpl vmClass = classLoader.loadClass(def.getInternalName());
        return vmClass.newInstance();
    }

    public void invokeExact(final ConstructorElement method, final VmObject instance, final List<Object> args) {
        getInstanceInvoker(method).invokeVoid(Vm.requireCurrentThread(), instance, args);
    }

    public Object invokeExact(final MethodElement method, final VmObject instance, final List<Object> args) {
        return getInstanceInvoker(method).invokeAny(Vm.requireCurrentThread(), instance, args);
    }

    public Object invokeVirtual(final MethodElement method, final VmObject instance, final List<Object> args) {
        return getVirtualInvoker(method, instance).invokeAny(Vm.requireCurrentThread(), instance, args);
    }

    @Override
    public VmObject newInstance(VmClass vmClass, ConstructorElement constructor, List<Object> arguments) throws Thrown {
        if (vmClass instanceof VmPrimitiveClass || vmClass instanceof VmArrayClass) {
            throw new IllegalArgumentException("Invalid class for construction");
        }
        LoadedTypeDefinition typeDefinition = vmClass.getTypeDefinition();
        if (typeDefinition == null || typeDefinition.isInterface() || typeDefinition.isAbstract()) {
            throw new IllegalArgumentException("Invalid class for construction");
        }
        LoadedTypeDefinition loaded = typeDefinition.load();
        if (constructor.getEnclosingType().load() != loaded) {
            throw new IllegalArgumentException("Constructor for wrong type specified");
        }
        VmInvokable invoker = getInstanceInvoker(constructor);
        VmObjectImpl vmObject = manuallyInitialize(((VmClassImpl) vmClass).newInstance());
        invoker.invoke(Vm.requireCurrentThread(), vmObject, arguments);
        return vmObject;
    }

    public void initialize(final VmClass vmClass) {
        VmThreadImpl vmThread = (VmThreadImpl) Vm.requireCurrentThread();
        VmClassLoader oldTCCL = vmThread.getContextClassLoader();
        try {
            if (oldTCCL == null) {
                VmClassLoader appCl = AppClassLoader.get(ctxt).getAppClassLoader();
                vmThread.setContextClassLoader(appCl);
            }
            ((VmClassImpl) vmClass).initialize(vmThread);
        } finally {
            vmThread.setContextClassLoader(oldTCCL);
        }
    }

    public void deliverSignal(final Signal signal) {

    }

    public VmObject allocateDirectBuffer(final ByteBuffer backingBuffer) {
        return null;
    }

    public DefinedTypeDefinition.Builder newTypeDefinitionBuilder(final VmClassLoader classLoader) {
        return classLoader.getClassContext().newTypeBuilder();
    }

    public VmObject getMainThreadGroup() {
        return mainThreadGroup;
    }

    private VmObject createMainThreadGroup() {
        // Create the System ThreadGroup
        VmClassImpl threadGroupClass = bootstrapClassLoader.loadClass("java/lang/ThreadGroup");
        VmObject mtg = manuallyInitialize(threadGroupClass.newInstance());
        LoadedTypeDefinition sgDef = threadGroupClass.getTypeDefinition();
        // Simulate the private constructor that is invoked by native code during VM startup at runtime
        mtg.getMemory().storeRef(mtg.indexOf(sgDef.findField("name")), intern("system"), SinglePlain);
        mtg.getMemory().store32(mtg.indexOf(sgDef.findField("maxPriority")), 10 /* Thread.MAX_PRIORITY */, SinglePlain);
        return mtg;
    }

    @Override
    public Memory allocate(ValueType type, long count) {
        if (count == 0) {
            return MemoryFactory.getEmpty();
        } else if (count == 1) {
            // todo: select upgradeLongs based on a class attribute
            return MemoryFactory.allocate(ctxt, type, count, true);
        } else {
            return MemoryFactory.replicate(allocate(type, 1), Math.toIntExact(count));
        }
    }

    @Override
    public VmClassLoaderImpl getClassLoaderForContext(ClassContext classContext) {
        VmClassLoaderImpl classLoader = (VmClassLoaderImpl) classContext.getClassLoader();
        return classLoader == null ? bootstrapClassLoader : classLoader;
    }

    @Override
    public void registerInvokable(ExecutableElement element, VmInvokable invokable) {
        DefinedTypeDefinition enclosingType = element.getEnclosingType();
        ClassContext classContext = enclosingType.getContext();
        VmClassLoaderImpl loader = getClassLoaderForContext(classContext);
        VmClassImpl vmClass = loader.getOrDefineClass(enclosingType.load());
        vmClass.registerInvokable(element, invokable);
    }

    private static final MethodHandle listToArray;
    private static final MethodHandle toPointerConversionRaw;
    private static final MethodHandle toLongConversion;

    static {
        final MethodHandle lta;
        final MethodHandle tpc;
        final MethodHandle tlc;
        try {
            lta = lookup().unreflect(List.class.getMethod("toArray"));
            tpc = lookup().unreflect(VmImpl.class.getDeclaredMethod("toPointerConversion", PointerType.class, Object.class));
            tlc = lookup().unreflect(VmImpl.class.getDeclaredMethod("toLongConversion", Object.class));
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new Error("Cannot initialize " + VmImpl.class, e);
        }
        listToArray = lta;
        toPointerConversionRaw = tpc;
        toLongConversion = tlc;
    }

    @Override
    public void registerHooks(VmClass clazz, Class<?> hookClass, MethodHandles.Lookup lookup) throws IllegalArgumentException {
        final LoadedTypeDefinition def = clazz.getTypeDefinition();
        final Constructor<?>[] ctors = hookClass.getDeclaredConstructors();
        if (ctors.length != 1) {
            throw new IllegalArgumentException("Expected exactly one constructor");
        }
        final Constructor<?> ctor = ctors[0];
        final List<Object> ctorArgs = new ArrayList<>(1);
        for (Class<?> parameterType : ctor.getParameterTypes()) {
            if (parameterType == Vm.class || parameterType == VmImpl.class) {
                ctorArgs.add(this);
            } else {
                throw new IllegalArgumentException("Invalid argument of " + parameterType + " on constructor of " + hookClass);
            }
        }
        MethodHandle ctorHandle;
        try {
            ctorHandle = lookup.unreflectConstructor(ctor);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Unable to access hook class constructor", e);
        }
        final Object instance;
        try {
            instance = ctorHandle.invokeWithArguments(ctorArgs);
        } catch (Throwable e) {
            throw new IllegalArgumentException("Unable to instantiate hook object", e);
        }
        final Method[] methods = hookClass.getDeclaredMethods();
        for (Method method : methods) {
            final Hook hook = method.getAnnotation(Hook.class);
            if (hook != null) {
                String name = hook.name();
                String finalName = name.isEmpty() ? method.getName() : name;
                String descStr = hook.descriptor();
                final InvokableElement me;
                if (descStr.isEmpty()) {
                    if (name.equals("<init>")) {
                        me = def.requireSingleConstructor(ce -> true);
                    } else {
                        me = def.requireSingleMethod(finalName);
                    }
                } else {
                    MethodDescriptor md = MethodDescriptor.parse(def.getContext(), ByteBuffer.wrap(descStr.getBytes(StandardCharsets.UTF_8)));
                    if (name.equals("<init>")) {
                        me = def.requireSingleConstructor(ce -> ce.getDescriptor().equals(md));
                    } else {
                        me = def.requireSingleMethod(m -> m.nameEquals(finalName) && m.getDescriptor().equals(md));
                    }
                }
                try {
                    final boolean hookIsStatic = Modifier.isStatic(method.getModifiers());
                    final MethodHandle handle = lookup.unreflect(method);
                    // adapt to VmInvokable; in the future we won't do this anymore (we'll call directly instead)
                    // we can't use the lambda metafactory unfortunately because we bind arguments
                    MethodHandle mappedHandle = mapHandleToInvokableForm(me.isStatic(), hookIsStatic, handle);
                    MethodHandle finalMappedHandle = hookIsStatic ? mappedHandle : mappedHandle.bindTo(instance);
                    VmInvokable inv = (thread, target, args) -> {
                        try {
                            return finalMappedHandle.invokeExact(thread, target, args);
                        } catch (Thrown t) {
                            throw t;
                        } catch (Throwable e) {
                            throw new Thrown(errorClass.newInstance("Internal error in interpreter hook: " + e));
                        }
                    };
                    registerInvokable(me, inv);
                } catch (Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalArgumentException("Unable to register hook method " + method, e);
                }
            }
        }
    }

    private MethodHandle mapHandleToInvokableForm(boolean targetIsStatic, boolean hookIsStatic, MethodHandle hookHandle) {
        // Four forms:
        // static xxx hook(thread, args...)
        // static xxx hook(thread, this, args...)
        // xxx hook(<instance>, thread, args...)
        // xxx hook(<instance>, thread, this, args...)
        int pos = hookIsStatic ? 2 : 3;
        hookHandle = hookHandle.asType(hookHandle.type().changeParameterType(pos - 2, VmThread.class));
        if (targetIsStatic) {
            // insert a {@code null} receiver
            hookHandle = MethodHandles.dropArguments(hookHandle, pos - 1, VmObject.class);
        } else {
            hookHandle = hookHandle.asType(hookHandle.type().changeParameterType(pos - 1, VmObject.class));
        }
        // we have to go backwards step by step from the user hook type towards VmInvokable
        // transform any pointer or long-typed arguments
        hookHandle = fixUpArguments(hookHandle, hookHandle.type(), pos);
        // this covers the array->positional arguments part
        hookHandle = hookHandle.asSpreader(pos, Object[].class, hookHandle.type().parameterCount() - pos);
        // this covers the list->array part
        hookHandle = MethodHandles.filterArguments(hookHandle, pos, listToArray);
        // transform the return type
        hookHandle = hookHandle.asType(hookHandle.type().changeReturnType(Object.class));
        // and that's it
        return hookHandle;
    }

    private MethodHandle fixUpArguments(MethodHandle handle, MethodType type, int idx) {
        if (idx == type.parameterCount()) {
            return handle;
        }
        final Class<?> pType = type.parameterType(idx);
        if (pType == long.class) {
            // input may be a Pointer or a Long, so narrow it if needed
            handle = MethodHandles.filterArguments(handle, idx, toLongConversion);
        } else if (pType == Pointer.class) {
            handle = MethodHandles.filterArguments(handle, idx, toPointerConversion);
        }
        return fixUpArguments(handle, type, idx + 1);
    }

    private static long toLongConversion(Object obj) {
        if (obj instanceof Long l) {
            return l.longValue();
        } else if (obj instanceof IntegerAsPointer p) {
            return p.getValue();
        } else if (obj == null) {
            throw new NullPointerException("Attempted conversion of a null pointer to " + Long.class.getName());
        } else {
            throw new ClassCastException("Cannot cast " + obj.getClass().getName() + " to " + Long.class.getName());
        }
    }

    private static Pointer toPointerConversion(PointerType type, Object obj) {
        if (obj instanceof Long l) {
            return new IntegerAsPointer(type, l.longValue());
        } else if (obj instanceof Pointer p) {
            return p;
        } else if (obj == null) {
            return null;
        } else {
            throw new ClassCastException("Cannot cast " + obj.getClass().getName() + " to " + Pointer.class.getName());
        }
    }

    @Override
    public VmPrimitiveClassImpl getPrimitiveClass(Primitive primitive) {
        switch (primitive) {
            case BOOLEAN: return booleanClass;
            case BYTE: return byteClass;
            case SHORT: return shortClass;
            case CHAR: return charClass;
            case INT: return intClass;
            case FLOAT: return floatClass;
            case LONG: return longClass;
            case DOUBLE: return doubleClass;
            case VOID: return voidClass;
            default: throw Assert.impossibleSwitchCase(primitive);
        }
    }

    @Override
    public VmObject createMethodType(final ClassContext classContext, final MethodDescriptor methodDescriptor) {
        VmClassLoaderImpl cl = getClassLoaderForContext(classContext);
        VmObject mt = cl.methodTypeCache.get(methodDescriptor);
        if (mt != null) {
            return mt;
        }
        VmClassImpl methodTypeClass = bootstrapClassLoader.loadClass("java/lang/invoke/MethodType");
        // construct it via factory method
        LoadedTypeDefinition mtDef = methodTypeClass.getTypeDefinition();
        int makeImplIdx = mtDef.findSingleMethodIndex(me -> me.nameEquals("makeImpl"));
        if (makeImplIdx == -1) {
            // bad JDK?
            throw new IllegalStateException();
        }
        MethodElement method = mtDef.getMethod(makeImplIdx);
        VmInvokable inv = methodTypeClass.getOrCompile(method);
        TypeDescriptor returnType = methodDescriptor.getReturnType();
        List<TypeDescriptor> parameterTypes = methodDescriptor.getParameterTypes();
        int size = parameterTypes.size();
        VmRefArrayImpl array = manuallyInitialize((VmRefArrayImpl) classClass.getArrayClass().newInstance(size));
        VmObject[] arrayArray = array.getArray();
        for (int i = 0; i < size; i ++) {
            arrayArray[i] = getClassForDescriptor(cl, parameterTypes.get(i));
        }
        mt = inv.invoke(Vm.requireCurrentThread(), null, List.of(getClassForDescriptor(cl, returnType), array, Boolean.valueOf(true)));
        VmObject appearing = cl.methodTypeCache.putIfAbsent(methodDescriptor, mt);
        if (appearing != null) {
            mt = appearing;
        }
        return mt;
    }

    @Override
    public VmObject createMethodType(ExecutableElement element) {
        final LoadedTypeDefinition enclosing = element.getEnclosingType().load();
        final MethodDescriptor methodDescriptor = element.getDescriptor();
        VmClassLoaderImpl cl = getClassLoaderForContext(enclosing.getContext());
        VmObject mt = cl.methodTypeCache.get(methodDescriptor);
        if (mt != null) {
            return mt;
        }
        // If the element's type contains an UnresolvedType, we can't create the MethodType object.
        // Leave the MethodType as null and an error will be raised at runtime if/when it is used.
        InvokableType it = element.getType();
        if (it.getReturnType() instanceof UnresolvedType) {
            return null;
        }
        for (ValueType paramType : it.getParameterTypes()) {
            if (paramType instanceof UnresolvedType) {
                return null;
            }
        }
        VmClassImpl methodTypeClass = bootstrapClassLoader.loadClass("java/lang/invoke/MethodType");
        // construct it via factory method
        LoadedTypeDefinition mtDef = methodTypeClass.getTypeDefinition();
        int makeImplIdx = mtDef.findSingleMethodIndex(me -> me.nameEquals("makeImpl"));
        if (makeImplIdx == -1) {
            // bad JDK?
            throw new IllegalStateException();
        }
        MethodElement method = mtDef.getMethod(makeImplIdx);
        VmInvokable inv = methodTypeClass.getOrCompile(method);
        TypeDescriptor returnType = methodDescriptor.getReturnType();
        List<TypeDescriptor> parameterTypes = methodDescriptor.getParameterTypes();
        int size = parameterTypes.size();
        VmRefArrayImpl array = manuallyInitialize((VmRefArrayImpl) classClass.getArrayClass().newInstance(size));
        VmObject[] arrayArray = array.getArray();
        for (int i = 0; i < size; i ++) {
            final TypeDescriptor descriptor = parameterTypes.get(i);
            if (descriptor == enclosing.getDescriptor()) {
                arrayArray[i] = enclosing.getVmClass();
            } else {
                arrayArray[i] = getClassForDescriptor(cl, descriptor);
            }
        }
        VmClass returnClass = returnType == enclosing.getDescriptor() ? enclosing.getVmClass() : getClassForDescriptor(cl, returnType);
        mt = inv.invoke(Vm.requireCurrentThread(), null, List.of(returnClass, array, Boolean.valueOf(true)));
        VmObject appearing = cl.methodTypeCache.putIfAbsent(methodDescriptor, mt);
        if (appearing != null) {
            mt = appearing;
        }
        return mt;
    }

    @Override
    public VmObject createMethodHandle(ClassContext classContext, VmClass caller, MethodHandleConstant constant) throws Thrown {
        Assert.checkNotNullParam("classContext", classContext);
        Assert.checkNotNullParam("constant", constant);
        // for this operation we have to call the factory method for a direct method handle, which means a VM thread must be active.
        VmClassLoaderImpl cl = getClassLoaderForContext(classContext);
        VmObject mh = cl.methodHandleCache.get(constant);
        if (mh != null) {
            return mh;
        }
        // generate the MethodName instance required by the factory
        VmClassImpl mnClass = bootstrapClassLoader.loadClass("java/lang/invoke/MemberName");
        LoadedTypeDefinition mnDef = mnClass.getTypeDefinition();
        VmClassImpl mhnClass = bootstrapClassLoader.loadClass("java/lang/invoke/MethodHandleNatives");
        LoadedTypeDefinition mhnDef = mhnClass.getTypeDefinition();
        String nameStr;
        VmObject type;
        AnnotatedElement resolvedElement;
        VmClassImpl owner = (VmClassImpl) getClassForDescriptor(cl, constant.getOwnerDescriptor());
        int extraFlags; // extra flags used by the MethodName API
        if (constant instanceof FieldMethodHandleConstant) {
            type = getClassForDescriptor(cl, ((FieldMethodHandleConstant) constant).getDescriptor());
            nameStr = ((FieldMethodHandleConstant) constant).getFieldName();
            resolvedElement = owner.getTypeDefinition().findField(nameStr);
            if (resolvedElement == null) {
                throw new Thrown(noSuchFieldErrorClass.newInstance());
            }
            extraFlags = 1 << 18;
        } else if (constant instanceof MethodMethodHandleConstant) {
            MethodMethodHandleConstant narrowed = (MethodMethodHandleConstant) constant;
            MethodDescriptor descriptor = narrowed.getDescriptor();
            type = createMethodType(classContext, descriptor);
            nameStr = narrowed.getMethodName();
            int mi = owner.getTypeDefinition().findMethodIndex(nameStr, descriptor);
            if (mi == -1) {
                throw new Thrown(noSuchMethodErrorClass.newInstance());
            }
            resolvedElement = owner.getTypeDefinition().getMethod(mi);
            extraFlags = 1 << 16;
        } else {
            assert constant instanceof ConstructorMethodHandleConstant;
            MethodDescriptor descriptor = ((ConstructorMethodHandleConstant) constant).getDescriptor();
            type = createMethodType(classContext, descriptor);
            nameStr = "<init>";
            int ci = owner.getTypeDefinition().findConstructorIndex(descriptor);
            if (ci == -1) {
                throw new Thrown(noSuchMethodErrorClass.newInstance());
            }
            resolvedElement = owner.getTypeDefinition().getConstructor(ci);
            extraFlags = 1 << 17;
        }
        VmStringImpl nameObj = intern(nameStr);
        // generate flags
        int kind = constant.getKind().getId();
        int modifiers = resolvedElement.getModifiers() & 0xffff; // only JVM-valid modifiers
        int flags = modifiers | extraFlags | (kind << 24);

        // instantiate the MemberName
        VmObject mn = manuallyInitialize(mnClass.newInstance());
        mn.getMemory().storeRef(mn.indexOf(mnDef.findField("clazz")), owner, SinglePlain);
        mn.getMemory().storeRef(mn.indexOf(mnDef.findField("name")), nameObj, SinglePlain);
        mn.getMemory().storeRef(mn.indexOf(mnDef.findField("type")), type, SinglePlain);
        mn.getMemory().store32(mn.indexOf(mnDef.findField("flags")), flags, SinglePlain);
        // resolve it
        // (List.of() forbids null values)
        mn = (VmObject) invokeExact(mhnDef.requireSingleMethod("resolve"), mn, Arrays.asList(
            mn,
            caller,
            Integer.valueOf(-1), // trusted
            Boolean.FALSE // no speculative resolve
        ));

        // call the factory method
        VmClassImpl dmhClass = bootstrapClassLoader.loadClass("java/lang/invoke/DirectMethodHandle");
        LoadedTypeDefinition dmhDef = dmhClass.getTypeDefinition();
        int makeIdx = dmhDef.findMethodIndex(me -> me.nameEquals("make") && me.getDescriptor().getParameterTypes().size() == 1);
        if (makeIdx == -1) {
            throw new IllegalStateException("No make() method found on DirectMethodHandle class");
        }
        mh = (VmObject) invokeExact(dmhDef.getMethod(makeIdx), null, List.of(mn));
        VmObject appearing = cl.methodHandleCache.putIfAbsent(constant, mh);
        return appearing != null ? appearing : mh;
    }

    private MethodElement valueOfMethod(VmClassImpl clazz, WordType argType) {
        clazz.initialize((VmThreadImpl) Vm.requireCurrentThread());
        LoadedTypeDefinition typeDef = clazz.getTypeDefinition();
        int mi = typeDef.findMethodIndex(me ->
            me.nameEquals("valueOf") && me.getType().getParameterType(0).equals(argType));
        if (mi == -1) {
            throw new IllegalStateException();
        }
        return typeDef.getMethod(mi);
    }

    @Override
    public VmObject box(ClassContext classContext, Literal literal) {
        Assert.checkNotNullParam("literal", literal);
        if (literal instanceof BooleanLiteral) {
            MethodElement booleanValueOf = valueOfMethod(bootstrapClassLoader.loadClass("java/lang/Boolean"), ((BooleanLiteral) literal).getType());
            return (VmObject) invokeExact(booleanValueOf, null, List.of(Boolean.valueOf(((BooleanLiteral) literal).booleanValue())));
        } else if (literal instanceof ByteArrayLiteral) {
            return newByteArray(((ByteArrayLiteral) literal).getValues());
        } else if (literal instanceof FloatLiteral) {
            FloatLiteral floatLiteral = (FloatLiteral) literal;
            FloatType type = floatLiteral.getType();
            if (type.getMinBits() == 32) {
                MethodElement floatValueOf = valueOfMethod(bootstrapClassLoader.loadClass("java/lang/Float"), type);
                return (VmObject) invokeExact(floatValueOf, null, List.of(Float.valueOf(floatLiteral.floatValue())));
            } else {
                MethodElement doubleValueOf = valueOfMethod(bootstrapClassLoader.loadClass("java/lang/Double"), type);
                return (VmObject) invokeExact(doubleValueOf, null, List.of(Double.valueOf(floatLiteral.doubleValue())));
            }
        } else if (literal instanceof IntegerLiteral) {
            // lots of possibilities here
            IntegerLiteral integerLiteral = (IntegerLiteral) literal;
            IntegerType type = integerLiteral.getType();
            if (type instanceof UnsignedIntegerType && type.getMinBits() == 16) {
                // the lone unsigned type
                MethodElement charValueOf = valueOfMethod(bootstrapClassLoader.loadClass("java/lang/Character"), type);
                return (VmObject) invokeExact(charValueOf, null, List.of(Character.valueOf(integerLiteral.charValue())));
            } else if (type.getMinBits() == 8) {
                MethodElement byteValueOf = valueOfMethod(bootstrapClassLoader.loadClass("java/lang/Byte"), type.asSigned());
                return (VmObject) invokeExact(byteValueOf, null, List.of(Byte.valueOf(integerLiteral.byteValue())));
            } else if (type.getMinBits() == 16) {
                MethodElement shortValueOf = valueOfMethod(bootstrapClassLoader.loadClass("java/lang/Short"), type); // already signed
                return (VmObject) invokeExact(shortValueOf, null, List.of(Short.valueOf(integerLiteral.shortValue())));
            } else if (type.getMinBits() == 32) {
                MethodElement intValueOf = valueOfMethod(bootstrapClassLoader.loadClass("java/lang/Integer"), type.asSigned());
                return (VmObject) invokeExact(intValueOf, null, List.of(Integer.valueOf(integerLiteral.intValue())));
            } else {
                assert type.getMinBits() == 64;
                MethodElement longValueOf = valueOfMethod(bootstrapClassLoader.loadClass("java/lang/Long"), type.asSigned());
                return (VmObject) invokeExact(longValueOf, null, List.of(Long.valueOf(integerLiteral.longValue())));
            }
        } else if (literal instanceof MethodHandleLiteral mhLiteral) {
            VmThreadImpl thread = (VmThreadImpl) Vm.requireCurrentThread();
            final Frame currentFrame = thread.currentFrame;
            final VmClass caller = currentFrame == null ? null : currentFrame.element.getEnclosingType().load().getVmClass();
            return createMethodHandle(classContext, caller, mhLiteral.getMethodHandleConstant());
        } else if (literal instanceof NullLiteral) {
            return null;
        } else if (literal instanceof ObjectLiteral) {
            return ((ObjectLiteral) literal).getValue();
        } else if (literal instanceof StringLiteral) {
            return intern(((StringLiteral) literal).getValue());
        } else if (literal instanceof TypeIdLiteral tl && tl.getValue() instanceof ClassObjectType cot) {
            return cot.getDefinition().load().getVmClass();
        } else {
            throw new UnsupportedOperationException("Boxing literal of type " + literal.getClass());
        }
    }

    @Override
    public VmReferenceArray newArrayOf(VmClass elementType, int size) {
        if (elementType instanceof VmPrimitiveClass) {
            throw new IllegalArgumentException("Cannot create a reference array with a primitive element type");
        }
        return (VmReferenceArray) manuallyInitialize(((VmClassImpl)elementType).getArrayClass().newInstance(size));
    }

    public VmReferenceArray newArrayOf(final VmClass elementType, final VmObject[] array) {
        if (elementType instanceof VmPrimitiveClass) {
            throw new IllegalArgumentException("Cannot create a reference array with a primitive element type");
        }
        VmRefArrayImpl obj = (VmRefArrayImpl) manuallyInitialize(((VmClassImpl) elementType).getArrayClass().newInstance(array.length));
        System.arraycopy(array, 0, obj.getArray(), 0, array.length);
        return obj;
    }

    @Override
    public VmByteArrayImpl newByteArray(byte[] array) {
        VmByteArrayImpl obj = manuallyInitialize(byteArrayClass.newInstance(array.length));
        System.arraycopy(array, 0, obj.getArray(), 0, array.length);
        return obj;
    }

    @Override
    public VmArray newCharArray(char[] array) {
        VmCharArrayImpl obj = manuallyInitialize(charArrayClass.newInstance(array.length));
        System.arraycopy(array, 0, obj.getArray(), 0, array.length);
        return obj;
    }

    @Override
    public VmArray newDoubleArray(double[] array) {
        VmDoubleArrayImpl obj = manuallyInitialize(doubleArrayClass.newInstance(array.length));
        System.arraycopy(array, 0, obj.getArray(), 0, array.length);
        return obj;
    }

    @Override
    public VmArray newFloatArray(float[] array) {
        VmFloatArrayImpl obj = manuallyInitialize(floatArrayClass.newInstance(array.length));
        System.arraycopy(array, 0, obj.getArray(), 0, array.length);
        return obj;
    }

    @Override
    public VmArray newIntArray(int[] array) {
        VmIntArrayImpl obj = manuallyInitialize(intArrayClass.newInstance(array.length));
        System.arraycopy(array, 0, obj.getArray(), 0, array.length);
        return obj;
    }

    @Override
    public VmArray newLongArray(long[] array) {
        VmLongArrayImpl obj = manuallyInitialize(longArrayClass.newInstance(array.length));
        System.arraycopy(array, 0, obj.getArray(), 0, array.length);
        return obj;
    }

    @Override
    public VmArray newShortArray(short[] array) {
        VmShortArrayImpl obj = manuallyInitialize(shortArrayClass.newInstance(array.length));
        System.arraycopy(array, 0, obj.getArray(), 0, array.length);
        return obj;
    }

    @Override
    public VmArray newBooleanArray(boolean[] array) {
        VmBooleanArrayImpl obj = manuallyInitialize(booleanArrayClass.newInstance(array.length));
        System.arraycopy(array, 0, obj.getArray(), 0, array.length);
        return obj;
    }

    @Override
    public VmObject getLookup(VmClass vmClass) {
        // todo: cache these on VmClassImpl?
        VmClassImpl lookupClass = bootstrapClassLoader.loadClass("java/lang/invoke/MethodHandles$Lookup");
        LoadedTypeDefinition lookupDef = lookupClass.getTypeDefinition();
        VmObjectImpl lookupObj = manuallyInitialize(lookupClass.newInstance());
        lookupObj.getMemory().storeRef(lookupObj.indexOf(lookupDef.findField("lookupClass")), vmClass, SinglePlain);
        int fullPower = MethodHandles.Lookup.PUBLIC |
            MethodHandles.Lookup.PRIVATE |
            MethodHandles.Lookup.PROTECTED |
            MethodHandles.Lookup.PACKAGE |
            MethodHandles.Lookup.MODULE;
        lookupObj.getMemory().store32(lookupObj.indexOf(lookupDef.findField("allowedModes")), fullPower, SinglePlain);
        VarHandle.releaseFence();
        return lookupObj;
    }

    @Override
    public VmThread[] getStartedThreads() {
        return startedThreads.toArray(VmThread[]::new);
    }

    public VmClassLoader getAppClassLoader() {
        try {
            LoadedTypeDefinition clDef = ctxt.getBootstrapClassContext().findDefinedType("jdk/internal/loader/ClassLoaders").load();
            VmClass classLoadersClass = clDef.getVmClass();
            return (VmClassLoader) classLoadersClass.getStaticMemory().loadRef(classLoadersClass.indexOfStatic(clDef.findField("APP_LOADER")), SinglePlain);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public VmClassLoader getPlatformClassLoader() {
        try {
            LoadedTypeDefinition clDef = ctxt.getBootstrapClassContext().findDefinedType("jdk/internal/loader/ClassLoaders").load();
            VmClass classLoadersClass = clDef.getVmClass();
            return (VmClassLoader) classLoadersClass.getStaticMemory().loadRef(classLoadersClass.indexOfStatic(clDef.findField("PLATFORM_LOADER")), SinglePlain);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public VmClass getClassForDescriptor(VmClassLoader cl, TypeDescriptor descriptor) {
        if (cl == null) {
            cl = bootstrapClassLoader;
        }
        if (descriptor instanceof BaseTypeDescriptor) {
            return getPrimitiveClass(Primitive.getPrimitiveFor((BaseTypeDescriptor) descriptor));
        } else if (descriptor instanceof ArrayTypeDescriptor) {
            return getClassForDescriptor(cl, ((ArrayTypeDescriptor) descriptor).getElementTypeDescriptor()).getArrayClass();
        } else {
            assert descriptor instanceof ClassTypeDescriptor;
            ClassTypeDescriptor ctd = (ClassTypeDescriptor) descriptor;
            String fullName = ctd.getPackageName().isEmpty() ? ctd.getClassName() : ctd.getPackageName() + '/' + ctd.getClassName();
            return cl.loadClass(fullName);
        }
    }

    VmStringImpl intern(VmStringImpl vmString) {
        String string = vmString.getContent();
        VmStringImpl existing = interned.get(string);
        if (existing != null) {
            return existing;
        }
        VmStringImpl appearing = interned.putIfAbsent(string, vmString);
        if (appearing != null) {
            return appearing;
        }
        return vmString;
    }

    public VmStringImpl intern(String string) {
        VmStringImpl vmString = interned.get(string);
        if (vmString == null) {
            vmString = new VmStringImpl(this, stringClass, string);
            manuallyInitialize(vmString);
            VmStringImpl appearing = interned.putIfAbsent(string, vmString);
            if (appearing != null) {
                vmString = appearing;
            }
        }
        return vmString;
    }

    public boolean isInternedString(VmObject val) {
        if (val instanceof VmStringImpl vs) {
            return interned.containsKey(vs.getContent());
        } else {
            return false;
        }
    }

    public void forEachInternedString(Consumer<VmString> thunk) {
        interned.forEach((s, vs) -> thunk.accept(vs));
    }

    public static VmImpl create(CompilationContext ctxt, Consumer<VmObject> manualInitializer) {
        return new VmImpl(Assert.checkNotNullParam("ctxt", ctxt), manualInitializer);
    }

    public VmImpl setPropertyDefines(List<String> propDefines) {
        this.propertyDefines = propDefines;
        return this;
    }

    List<String> getPropertyDefines() {
        return propertyDefines;
    }

    static VmImpl require() {
        return (VmImpl) Vm.requireCurrent();
    }

    private VmInvokable getInstanceInvoker(ExecutableElement element) {
        DefinedTypeDefinition enclosingType = element.getEnclosingType();
        VmClassImpl loadedClass = (VmClassImpl) enclosingType.load().getVmClass();
        return loadedClass.getOrCompile(element);
    }

    private VmInvokable getVirtualInvoker(MethodElement element, VmObject instance) {
        VmClassImpl instanceClass = (VmClassImpl) instance.getVmClass();
        LoadedTypeDefinition instanceDef = instanceClass.getTypeDefinition();
        MethodElement target = instanceDef.resolveMethodElementVirtual(instanceDef.getContext(), element.getName(), element.getDescriptor(), true);
        if (target == null) {
            throw new Thrown(noSuchMethodErrorClass.newInstance(element.getName()));
        }
        return instanceClass.getOrCompile(target);
    }

    public Memory getGlobal(final GlobalVariableElement variableElement) {
        Memory memory = globals.get(variableElement);
        if (memory == null) {
            memory = allocate(variableElement.getType(), 1);
            Memory appearing = globals.putIfAbsent(variableElement, memory);
            if (appearing != null) {
                memory = appearing;
            }
        }
        return memory;
    }

    <T extends VmObject> T manuallyInitialize(final T object) {
        manualInitializers.accept(object);
        return object;
    }
}

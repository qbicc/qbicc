package org.qbicc.interpreter.impl;

import static java.lang.invoke.MethodHandles.lookup;
import static org.qbicc.graph.atomic.AccessModes.SinglePlain;
import static org.qbicc.graph.atomic.AccessModes.SingleRelease;

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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.zip.CRC32;

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
import org.qbicc.graph.literal.TypeLiteral;
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
import org.qbicc.type.PointerType;
import org.qbicc.type.Primitive;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.AnnotatedElement;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NestedClassElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.Signature;
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
        ctxt.getExceptionField();
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
        toStringMethod = objectClass.getTypeDefinition().resolveMethodElementExact("toString", MethodDescriptor.synthesize(bcc, stringDef.getDescriptor(), List.of()));
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
            VmClassImpl strictMathClass = bootstrapClassLoader.loadClass("java/lang/StrictMath");
            strictMathClass.registerInvokable("sin", ((thread, target, args) -> Double.valueOf(StrictMath.sin(((Double) args.get(0)).doubleValue()))));
            strictMathClass.registerInvokable("cos", ((thread, target, args) -> Double.valueOf(StrictMath.cos(((Double) args.get(0)).doubleValue()))));
            strictMathClass.registerInvokable("tan", ((thread, target, args) -> Double.valueOf(StrictMath.tan(((Double) args.get(0)).doubleValue()))));
            strictMathClass.registerInvokable("asin", ((thread, target, args) -> Double.valueOf(StrictMath.asin(((Double) args.get(0)).doubleValue()))));
            strictMathClass.registerInvokable("acos", ((thread, target, args) -> Double.valueOf(StrictMath.acos(((Double) args.get(0)).doubleValue()))));
            strictMathClass.registerInvokable("atan", ((thread, target, args) -> Double.valueOf(StrictMath.atan(((Double) args.get(0)).doubleValue()))));
            strictMathClass.registerInvokable("log", ((thread, target, args) -> Double.valueOf(StrictMath.log(((Double) args.get(0)).doubleValue()))));
            strictMathClass.registerInvokable("log10", ((thread, target, args) -> Double.valueOf(StrictMath.log10(((Double) args.get(0)).doubleValue()))));
            strictMathClass.registerInvokable("sqrt", ((thread, target, args) -> Double.valueOf(StrictMath.sqrt(((Double) args.get(0)).doubleValue()))));
            strictMathClass.registerInvokable("IEEEremainder", ((thread, target, args) -> Double.valueOf(StrictMath.IEEEremainder(((Double) args.get(0)).doubleValue(), ((Double) args.get(1)).doubleValue()))));
            strictMathClass.registerInvokable("atan2", ((thread, target, args) -> Double.valueOf(StrictMath.atan2(((Double) args.get(0)).doubleValue(), ((Double) args.get(1)).doubleValue()))));
            strictMathClass.registerInvokable("sinh", ((thread, target, args) -> Double.valueOf(StrictMath.sinh(((Double) args.get(0)).doubleValue()))));
            strictMathClass.registerInvokable("cosh", ((thread, target, args) -> Double.valueOf(StrictMath.cosh(((Double) args.get(0)).doubleValue()))));
            strictMathClass.registerInvokable("tanh", ((thread, target, args) -> Double.valueOf(StrictMath.tanh(((Double) args.get(0)).doubleValue()))));
            strictMathClass.registerInvokable("expm1", ((thread, target, args) -> Double.valueOf(StrictMath.expm1(((Double) args.get(0)).doubleValue()))));
            strictMathClass.registerInvokable("log1p", ((thread, target, args) -> Double.valueOf(StrictMath.log1p(((Double) args.get(0)).doubleValue()))));

            // String
            VmClassImpl stringClass = bootstrapClassLoader.loadClass("java/lang/String");

            stringClass.registerInvokable("intern", (thread, target, args) -> intern((VmStringImpl) target));
            // for performance:
            // String.hashCode is well-defined
            stringClass.registerInvokable("hashCode", (thread, target, args) -> Integer.valueOf(((VmStringImpl) target).getContent().hashCode()));
            stringClass.registerInvokable("equals", (thread, target, args) -> Boolean.valueOf(args.get(0) instanceof VmStringImpl other && ((VmStringImpl)target).contentEquals(other.getContent())));
            stringClass.registerInvokable("coder", (thread, target, args) -> Byte.valueOf((byte) target.getMemory().load8(stringCoderOffset, SinglePlain)));
            stringClass.registerInvokable("isLatin1", (thread, target, args) -> Boolean.valueOf(target.getMemory().load8(stringCoderOffset, SinglePlain) == 0));
            stringClass.registerInvokable("length", (thread, target, args) -> Integer.valueOf(((VmStringImpl) target).getContent().length()));
            stringClass.registerInvokable("charAt", (thread, target, args) -> {
                try {
                    return Integer.valueOf(((VmStringImpl) target).getContent().charAt(((Integer) args.get(0)).intValue()));
                } catch (StringIndexOutOfBoundsException e) {
                    VmThrowableClassImpl ioobe = (VmThrowableClassImpl) bootstrapClassLoader.loadClass("java/lang/StringIndexOutOfBoundsException");
                    throw new Thrown(ioobe.newInstance(e.getMessage()));
                }
            });

            // Thread
            VmClassImpl threadNativeClass = bootstrapClassLoader.loadClass("java/lang/Thread");
            threadNativeClass.registerInvokable("yield", (thread, target, args) -> {
                Thread.yield();
                return null;
            });
            threadNativeClass.registerInvokable("start", (thread, target, args) -> {
                startedThreads.add(vmThread);
                return null;
            });

            // Throwable
            VmClassImpl throwableClass = bootstrapClassLoader.loadClass("java/lang/Throwable");

            idx = throwableClass.getTypeDefinition().findSingleMethodIndex(me -> me.nameEquals("fillInStackTrace") && me.getParameters().size() == 1);
            throwableClass.registerInvokable(throwableClass.getTypeDefinition().getMethod(idx), (thread, target, args) -> {
                ((VmThrowableImpl)target).fillInStackTrace();
                return target;
            });

            // Class
            VmClassImpl classClass = bootstrapClassLoader.loadClass("java/lang/Class");
            classClass.registerInvokable("getModifiers", (thread, target, args) -> ((VmClass)target).getTypeDefinition().getModifiers());
            classClass.registerInvokable("getSuperclass", (thread, target, args) -> {
                LoadedTypeDefinition sc = ((VmClass)target).getTypeDefinition().getSuperClass();
                return sc == null ? null : sc.getVmClass();
            });
            classClass.registerInvokable("isArray", (thread, target, args) -> {
                VmClassImpl clazz = (VmClassImpl) target;
                return clazz instanceof VmArrayClass;
            });
            classClass.registerInvokable("isHidden", (thread, target, args) -> {
                VmClassImpl clazz = (VmClassImpl) target;
                return clazz.getTypeDefinition().isHidden();
            });

            classClass.registerInvokable("isInterface", (thread, target, args) ->
                Boolean.valueOf(! (target instanceof VmPrimitiveClass) && ((VmClassImpl) target).getTypeDefinition().isInterface()));
            classClass.registerInvokable("isAssignableFrom", (thread, target, args) -> {
                VmClassImpl lhs = (VmClassImpl) target;
                VmClassImpl rhs = (VmClassImpl)args.get(0);
                return Boolean.valueOf(lhs.isAssignableFrom(rhs));
            });
            classClass.registerInvokable("isPrimitive", (thread, target, args) -> Boolean.valueOf(target instanceof VmPrimitiveClass));
            classClass.registerInvokable("getEnclosingMethod0", (thread, target, args) -> {
                if (target instanceof VmPrimitiveClass) {
                    return null;
                }
                LoadedTypeDefinition def = ((VmClassImpl) target).getTypeDefinition();
                LoadedTypeDefinition emcDef = def.getEnclosingMethodClass();
                if (emcDef == null) {
                    return null;
                }
                VmImpl vm = (VmImpl) thread.getVM();
                VmRefArrayClassImpl arrayClass = (VmRefArrayClassImpl) vm.objectClass.getArrayClass();
                VmRefArrayImpl vmArray = arrayClass.newInstance(3);
                ClassContext emcCtxt = emcDef.getContext();
                VmClassLoaderImpl emcLoader = vm.getClassLoaderForContext(emcCtxt);
                VmClassImpl emc = emcLoader.loadClass(emcDef.getInternalName());
                VmObject[] realArray = vmArray.getArray();
                realArray[0] = emc;
                MethodElement enclosingMethod = def.getEnclosingMethod();
                if (enclosingMethod != null) {
                    realArray[1] = vm.intern(enclosingMethod.getName());
                    realArray[2] = vm.intern(enclosingMethod.getDescriptor().toString());
                }
                return vmArray;
            });
            // todo: this one probably should just be a single field on Class
            classClass.registerInvokable("getDeclaringClass0", (thread, target, args) -> {
                if (target instanceof VmPrimitiveClass) {
                    return null;
                }
                LoadedTypeDefinition def = ((VmClassImpl) target).getTypeDefinition();
                NestedClassElement enc = def.getEnclosingNestedClass();
                if (enc != null) {
                    DefinedTypeDefinition enclosingType = enc.getEnclosingType();
                    if (enclosingType != null) {
                        VmImpl vm = (VmImpl) thread.getVM();
                        VmClassLoaderImpl loader = vm.getClassLoaderForContext(enclosingType.getContext());
                        return loader.loadClass(enclosingType.getInternalName());
                    }
                }
                return null;
            });
            classClass.registerInvokable("getInterfaces0", (thread, target, args) -> {
                LoadedTypeDefinition ltd = ((VmClassImpl)target).getTypeDefinition();
                LoadedTypeDefinition[] ltdInt = ltd.getInterfaces();
                VmClass[] interfaces = new VmClass[ltdInt.length];
                for (int i=0; i<ltdInt.length; i++) {
                    interfaces[i] = ltdInt[i].getVmClass();
                }
                VmArray ans = newArrayOf(classClass, interfaces);
                return ans;
            });
            classClass.registerInvokable("isInstance", (thread, target, args) -> {
                VmClassImpl clazz = (VmClassImpl) target;
                VmObject obj = (VmObject) args.get(0);
                if (obj == null) {
                    return Boolean.FALSE;
                }
                VmClass objClazz = obj.getVmClass();
                return Boolean.valueOf(objClazz.getInstanceObjectType().isSubtypeOf(clazz.getInstanceObjectType()));
            });
            classClass.registerInvokable("forName0", (thread, target, args) -> {
                VmClassLoaderImpl classLoader = (VmClassLoaderImpl) args.get(2);
                if (classLoader == null) {
                    classLoader = bootstrapClassLoader;
                }
                String name = ((VmStringImpl) args.get(0)).getContent();
                int dims = 0;
                while (name.startsWith("[[")) {
                    name = name.substring(1);
                    dims ++;
                }
                if (name.startsWith("[L")) {
                    if (! name.endsWith(";")) {
                        throw new Thrown(noClassDefFoundErrorClass.newInstance("Bad array descriptor"));
                    }
                    // load the array class
                    name = name.substring(2, name.length() - 1);
                    dims ++;
                }
                VmClassImpl clazz = classLoader.loadClass(name.replace('.', '/'));
                for (int i = 0; i < dims; i ++) {
                    clazz = clazz.getArrayClass();
                }
                if (((Boolean) args.get(1)).booleanValue()) {
                    clazz.initialize((VmThreadImpl) thread);
                }
                return clazz;
            });
            classClass.registerInvokable("getGenericSignature0", (thread, target, args) -> {
                LoadedTypeDefinition ltd = ((VmClass) target).getTypeDefinition();
                Signature sig = ltd.getSignature();
                return intern(sig.toString());
            });

            VmClassImpl classloaderClass = bootstrapClassLoader.loadClass("java/lang/ClassLoader");
            classloaderClass.registerInvokable("defineClass1", (thread, target, args) -> {
                VmClassLoaderImpl classLoader = (VmClassLoaderImpl) args.get(0);
                VmString name = fixClassname((VmString) args.get(1));
                VmByteArrayImpl b = (VmByteArrayImpl) args.get(2);
                int off = (Integer) args.get(3);
                int len = (Integer) args.get(4);
                VmObject pd = (VmObject) args.get(5);
                VmString source = (VmString) args.get(6);
                if (off != 0 || len != b.getLength()) {
                    b = b.copyOfRange(off, len);
                }
                if (classLoader == null) {
                    classLoader = bootstrapClassLoader;
                }
                return classLoader.defineClass(name, b, pd);
            });
            FieldElement classDataField = classClass.getTypeDefinition().findField("classData");
            classloaderClass.registerInvokable("defineClass0", (thread, target, args) -> {
                VmClassLoaderImpl classLoader = (VmClassLoaderImpl) args.get(0);
                VmClassImpl lookup = (VmClassImpl) args.get(1);
                VmString name = fixClassname((VmString) args.get(2));
                VmByteArrayImpl b = (VmByteArrayImpl) args.get(3);
                int off = ((Integer) args.get(4)).intValue();
                int len = ((Integer) args.get(5)).intValue();
                int flags = ((Integer) args.get(8)).intValue();
                VmObject data = (VmObject) args.get(9);
                if (off != 0 || len != b.getLength()) {
                    b = b.copyOfRange(off, len);
                }
                if (classLoader == null) {
                    classLoader = bootstrapClassLoader;
                }
                boolean nestMate = (flags & 1) != 0;
                boolean hidden = (flags & 2) != 0;
                VmClassImpl defined = classLoader.defineClass(name, b, hidden);
                if (nestMate) {
                    VmClassImpl host = lookup.getNestHost();
                    host.addNestMember(defined);
                    defined.setNestHost(host);
                }
                defined.getMemory().storeRef(defined.indexOf(classDataField), data, SingleRelease);
                return defined;
            });
            classloaderClass.registerInvokable("findBootstrapClass", (thread, target, args) -> {
                DefinedTypeDefinition definedType = ctxt.getBootstrapClassContext().findDefinedType(((VmString) args.get(0)).getContent().replace('.', '/'));
                return definedType == null ? null : definedType.load().getVmClass();
            });
            classloaderClass.registerInvokable("findLoadedClass0", (thread, target, args) -> {
                VmClassLoaderImpl classLoader = (VmClassLoaderImpl) target;
                VmString name = (VmString) args.get(0);
                return classLoader.findLoadedClass(name.getContent());
            });

            VmClassImpl builtinLoader = bootstrapClassLoader.loadClass("jdk/internal/loader/BuiltinClassLoader");
            builtinLoader.registerInvokable("findClassOnClassPathOrNull", (thread, target, args) -> {
                VmString name = fixClassname((VmString) args.get(0));
                VmClassLoaderImpl cl =(VmClassLoaderImpl) target;
                DefinedTypeDefinition definedType = cl.getClassContext().findDefinedType(name.getContent());
                if (definedType == null) {
                    return null;
                }
                try {
                    return definedType.load().getVmClass();
                } catch (Exception e) {
                    return null;
                }
            });

            // Module
            VmClassImpl moduleClass = bootstrapClassLoader.loadClass("java/lang/Module");
            FieldElement moduleLoaderField = moduleClass.getTypeDefinition().findField("loader", true);
            moduleClass.registerInvokable("defineModule0", (thread, target, args) -> {
                VmObjectImpl module = (VmObjectImpl) args.get(0);
                boolean isOpen = ((Boolean) args.get(1)).booleanValue();
                VmStringImpl versionObj = (VmStringImpl) args.get(2);
                VmStringImpl locationObj = (VmStringImpl) args.get(3);
                VmRefArrayImpl packageNames = (VmRefArrayImpl) args.get(4);
                VmClassLoaderImpl loader = (VmClassLoaderImpl) module.getMemory().loadRef(moduleClass.indexOf(moduleLoaderField), SinglePlain);
                if (loader == null) {
                    loader = bootstrapClassLoader;
                }
                for (VmObject vmObject : packageNames.getArray()) {
                    VmStringImpl packageNameObj = (VmStringImpl) vmObject;
                    String packageName = packageNameObj.getContent();
                    loader.setModulePackage(packageName, module);
                }
                return null;
            });
            moduleClass.registerInvokable("addReads0", (thread, target, args) -> null);
            moduleClass.registerInvokable("addExports0", (thread, target, args) -> null);
            moduleClass.registerInvokable("addExportsToAll0", (thread, target, args) -> null);
            moduleClass.registerInvokable("addExportsToAllUnnamed0", (thread, target, args) -> null);

            // Array
            VmClassImpl arrayClass = bootstrapClassLoader.loadClass("java/lang/reflect/Array");
            arrayClass.registerInvokable("newArray", (thread, target, args) -> {
                VmClassImpl componentType = (VmClassImpl)args.get(0);
                int length = (Integer)args.get(1);
                return manuallyInitialize(componentType.getArrayClass().newInstance(length));
            });

            VmClassImpl reflectClass = bootstrapClassLoader.loadClass("jdk/internal/reflect/Reflection");
            reflectClass.registerInvokable("getCallerClass", (thread, target, args) -> {
                Frame currentFrame = ((VmThreadImpl)thread).currentFrame;
                Frame enclosing = currentFrame.enclosing;
                while (enclosing.element.getEnclosingType().getInternalName().equals("java/lang/reflect/Method") || enclosing.element.hasAllModifiersOf(ClassFile.I_ACC_HIDDEN)) {
                    enclosing = enclosing.enclosing;
                }
                DefinedTypeDefinition def = enclosing.element.getEnclosingType();
                return def.load().getVmClass();
            });
            reflectClass.registerInvokable("getClassAccessFlags", (thread, target, args) -> {
                VmClassImpl cls = (VmClassImpl)args.get(0);
                return cls.getTypeDefinition().getModifiers() & 0x1FFF;
            });

            // OSEnvironment
            VmClassImpl osEnvClass = bootstrapClassLoader.loadClass("jdk/internal/misc/OSEnvironment");
            osEnvClass.registerInvokable("initialize", (thread, target, args) -> null); // Skip this for build-time init.

            VmClassImpl unixDispatcher = bootstrapClassLoader.loadClass("sun/nio/fs/UnixNativeDispatcher");
            unixDispatcher.registerInvokable("getcwd", (thread, target, args) -> newByteArray(System.getProperty("user.dir").getBytes()));

            // FileDescriptor
            VmClassImpl fdClass = bootstrapClassLoader.loadClass("java/io/FileDescriptor");
            fdClass.registerInvokable("getAppend", (thread, target, args) -> Boolean.FALSE);
            fdClass.registerInvokable("getHandle", (thread, target, args) -> Long.valueOf(-1));

            // ProcessEnvironment
            VmClassImpl processEnvClass = bootstrapClassLoader.loadClass("java/lang/ProcessEnvironment");
            processEnvClass.registerInvokable("getHostEnvironment", (thread, target, args) -> {
                // todo: customize
                VmReferenceArray array = (VmReferenceArray) stringClass.getArrayClass().newInstance(4);
                VmObject[] arrayArray = array.getArray();
                int i = 0;
                for (String str : List.of(
                    "TZ", TimeZone.getDefault().getDisplayName(),
                    "LANG", Locale.getDefault().toLanguageTag() + "." + Charset.defaultCharset().name()
                )) {
                    arrayArray[i++] = intern(str);
                }
                return array;
            });

            // Build
            VmClassImpl build = bootstrapClassLoader.loadClass("org/qbicc/runtime/Build");
            build.registerInvokable("isHost", (thread, target, args) -> Boolean.TRUE);
            build.registerInvokable("isTarget", (thread, target, args) -> Boolean.FALSE);

            // CRC32
            VmClassImpl crc32 = bootstrapClassLoader.loadClass("java/util/zip/CRC32");
            crc32.registerInvokable("reset", (thread, target, args) -> {
                VmObjectImpl t = (VmObjectImpl) target;
                CRC32 crc = t.getOrAddAttachment(CRC32.class, CRC32::new);
                crc.reset();
                t.setIntField(crc32.getTypeDefinition(), "crc", 0);
                return null;
            });
            crc32.registerInvokable("update", 3, (thread, target, args) -> {
                VmObjectImpl t = (VmObjectImpl) target;
                VmByteArrayImpl a = (VmByteArrayImpl) args.get(0);
                int off = ((Integer) args.get(1)).intValue();
                int len = ((Integer) args.get(2)).intValue();
                CRC32 crc = t.getOrAddAttachment(CRC32.class, CRC32::new);
                crc.update(a.getArray(), off, len);
                t.setIntField(crc32.getTypeDefinition(), "crc", (int) crc.getValue());
                return null;
            });
            crc32.registerInvokable("update", MethodDescriptor.synthesize(
                bootstrapClassLoader.getClassContext(),
                    BaseTypeDescriptor.V,
                    List.of(BaseTypeDescriptor.I)
                ),
                (thread, target, args) -> {
                    VmObjectImpl t = (VmObjectImpl) target;
                    int val = ((Integer) args.get(0)).intValue() & 0xff;
                    CRC32 crc = t.getOrAddAttachment(CRC32.class, CRC32::new);
                    crc.update(val);
                    t.setIntField(crc32.getTypeDefinition(), "crc", (int) crc.getValue());
                    return null;
                }
            );

            // RNG
            VmClassImpl seedGenerator = bootstrapClassLoader.loadClass("sun/security/provider/SeedGenerator");
            seedGenerator.registerInvokable("getSystemEntropy", (thread, target, args) ->
                thread.getVM().newByteArray(new SecureRandom().generateSeed(20))
            );
            seedGenerator.registerInvokable("generateSeed", (thread, target, args) -> {
                VmByteArrayImpl bytes = (VmByteArrayImpl) args.get(0);
                byte[] seed = new SecureRandom().generateSeed(bytes.getLength());
                for (int i=0; i<seed.length; i++) {
                    bytes.getArray()[i] = seed[i];
                }
                return null;
            });

            /////////////////
            // TODO: temporary workaround for var/method handle initialization
            // Make access check methods always return true
            VmClassImpl verifyAccess = bootstrapClassLoader.loadClass("sun/invoke/util/VerifyAccess");
            verifyAccess.registerInvokable("isClassAccessible", (thread, target, args) -> Boolean.TRUE);
            VmClassImpl lookup = bootstrapClassLoader.loadClass("java/lang/invoke/MethodHandles$Lookup");
            lookup.registerInvokable("checkAccess", (thread, target, args) -> null);
            lookup.registerInvokable("checkMethod", (thread, target, args) -> null);
            lookup.registerInvokable("checkField", (thread, target, args) -> null);
            // TODO: ↑ ↑ remove this ↑ ↑

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
    private VmString fixClassname(VmString name) {
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
        vmThread.setPriority(priority);
        return vmThread;
    }

    public DefinedTypeDefinition loadClass(ClassContext classContext, final String name) throws Thrown {
        VmClass loaded = getClassLoaderForContext(classContext).loadClass(name);
        return loaded == null ? null : loaded.getTypeDefinition();
    }

    public byte[] loadResource(final ClassContext classContext, final String name) throws Thrown {
        // todo: implement by calling getAllBytes() on the class loader resource stream
        return null;
    }

    public List<byte[]> loadResources(final ClassContext classContext, final String name) throws Thrown {
        // todo: implement by calling getAllBytes() on the class loader resource streams
        return List.of();
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
        final Method[] methods = hookClass.getDeclaredMethods();
        final Object instance;
        try {
            instance = ctor.newInstance(ctorArgs.toArray());
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalArgumentException("Unable to instantiate hook object", e);
        }
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
    public VmObject createMethodHandle(ClassContext classContext, MethodHandleConstant constant) throws Thrown {
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
            null, // caller unused in our impl (todo: add caller arg to VM.createMethodHandle)
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
        } else if (literal instanceof MethodHandleLiteral) {
            MethodHandleLiteral mhLiteral = (MethodHandleLiteral) literal;
            return createMethodHandle(classContext, mhLiteral.getMethodHandleConstant());
        } else if (literal instanceof NullLiteral) {
            return null;
        } else if (literal instanceof ObjectLiteral) {
            return ((ObjectLiteral) literal).getValue();
        } else if (literal instanceof StringLiteral) {
            return intern(((StringLiteral) literal).getValue());
        } else if (literal instanceof TypeLiteral tl && tl.getValue() instanceof ClassObjectType cot) {
            return cot.getDefinition().load().getVmClass();
        } else {
            throw new UnsupportedOperationException("Boxing literal of type " + literal.getClass());
        }
    }

    @Override
    public Object boxThin(ClassContext classContext, Literal literal) {
        Assert.checkNotNullParam("literal", literal);
        if (literal instanceof BooleanLiteral bl) {
            return Boolean.valueOf(bl.booleanValue());
        } else if (literal instanceof ByteArrayLiteral) {
            byte[] values = ((ByteArrayLiteral) literal).getValues();
            return newByteArray(values.clone());
        } else if (literal instanceof FloatLiteral fl) {
            FloatType type = fl.getType();
            if (type.getMinBits() == 32) {
                return Float.valueOf(fl.floatValue());
            } else {
                return Double.valueOf(fl.doubleValue());
            }
        } else if (literal instanceof IntegerLiteral il) {
            // lots of possibilities here
            IntegerType type = il.getType();
            if (type instanceof UnsignedIntegerType && type.getMinBits() == 16) {
                return Character.valueOf(il.charValue());
            } else if (type.getMinBits() == 8) {
                return Byte.valueOf(il.byteValue());
            } else if (type.getMinBits() == 16) {
                return Short.valueOf(il.shortValue());
            } else if (type.getMinBits() == 32) {
                return Integer.valueOf(il.intValue());
            } else {
                assert type.getMinBits() == 64;
                return Long.valueOf(il.longValue());
            }
        } else if (literal instanceof MethodHandleLiteral mhLiteral) {
            return createMethodHandle(classContext, mhLiteral.getMethodHandleConstant());
        } else if (literal instanceof NullLiteral) {
            return null;
        } else if (literal instanceof ObjectLiteral ol) {
            return ol.getValue();
        } else if (literal instanceof StringLiteral sl) {
            return intern(sl.getValue());
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
        MethodElement target = instanceDef.resolveMethodElementVirtual(element.getName(), element.getDescriptor(), true);
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

package org.qbicc.interpreter.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ByteArrayLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.MethodHandleLiteral;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.Signal;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmArrayClass;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmInvokable;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmPrimitiveClass;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThread;
import org.qbicc.machine.arch.Platform;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.Primitive;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.AnnotatedElement;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NestedClassElement;
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
    private final Map<GlobalVariableElement, MemoryImpl> globals = new ConcurrentHashMap<>();
    private final Map<String, VmStringImpl> interned = new ConcurrentHashMap<>();
    private final VmClassLoaderImpl bootstrapClassLoader;
    private final AtomicBoolean initialized = new AtomicBoolean();
    private final Consumer<VmObject> manualInitializers;

    final MemoryImpl emptyMemory;

    boolean bootstrapComplete;

    // core classes
    final VmClassImpl objectClass;
    final VmClassClassImpl classClass;
    final VmStringClassImpl stringClass;
    final int stringCoderOffset;
    final int stringValueOffset;
    final VmThreadClassImpl threadClass;
    final VmThrowableClassImpl throwableClass;
    final VmClassLoaderClassImpl classLoaderClass;

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

    // error classes
    final VmThrowableClassImpl errorClass;

    final VmThrowableClassImpl linkageErrorClass;

    final VmThrowableClassImpl incompatibleClassChangeErrorClass;
    final VmThrowableClassImpl noClassDefFoundErrorClass;

    final VmThrowableClassImpl noSuchFieldErrorClass;
    final VmThrowableClassImpl noSuchMethodErrorClass;

    final VmClassImpl stackTraceElementClass;

    // jli special classes
    final VmMemberNameClassImpl memberNameClass;

    // regular classes
    volatile VmClassImpl propertiesClass;

    volatile MethodElement setPropertyMethod;

    volatile VmObject mainThreadGroup;

    final Set<VmThreadImpl> startedThreads = ConcurrentHashMap.newKeySet();

    VmImpl(final CompilationContext ctxt, Consumer<VmObject> manualInitializers) {
        this.ctxt = ctxt;
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
        classLoaderClass = new VmClassLoaderClassImpl(this, bcc.findDefinedType("java/lang/ClassLoader").load(), null);
        LoadedTypeDefinition stringDef = bcc.findDefinedType("java/lang/String").load();
        stringClass = new VmStringClassImpl(this, stringDef);
        FieldElement coderField = stringDef.findField("coder");
        FieldElement valueField = stringDef.findField("value");
        Layout layout = Layout.getForInterpreter(ctxt);
        LayoutInfo stringLayout = layout.getInstanceLayoutInfo(stringDef);
        stringCoderOffset = stringLayout.getMember(coderField).getOffset();
        stringValueOffset = stringLayout.getMember(valueField).getOffset();
        threadClass = new VmThreadClassImpl(this, bcc.findDefinedType("java/lang/Thread").load(), null);
        throwableClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/Throwable").load(), null);

        byteClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getByteArrayTypeDefinition(), "byte", BaseTypeDescriptor.B);
        shortClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getShortArrayTypeDefinition(), "short", BaseTypeDescriptor.S);
        intClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getIntArrayTypeDefinition(), "int", BaseTypeDescriptor.I);
        longClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getLongArrayTypeDefinition(), "long", BaseTypeDescriptor.J);
        floatClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getFloatArrayTypeDefinition(), "float", BaseTypeDescriptor.F);
        doubleClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getDoubleArrayTypeDefinition(), "double", BaseTypeDescriptor.D);
        charClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getCharArrayTypeDefinition(), "char", BaseTypeDescriptor.C);
        booleanClass = new VmPrimitiveClassImpl(this, classClass, coreClasses.getBooleanArrayTypeDefinition(), "boolean", BaseTypeDescriptor.Z);
        voidClass = new VmPrimitiveClassImpl(this, classClass, null, "void", BaseTypeDescriptor.V);

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

        // throwables
        errorClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/Error").load(), null);
        errorClass.postConstruct(this);

        // errors
        linkageErrorClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/LinkageError").load(), null);
        linkageErrorClass.postConstruct(this);

        // linkage errors
        incompatibleClassChangeErrorClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/IncompatibleClassChangeError").load(), null);
        incompatibleClassChangeErrorClass.postConstruct(this);
        noClassDefFoundErrorClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/NoClassDefFoundError").load(), null);
        noClassDefFoundErrorClass.postConstruct(this);

        // incompatible class change errors
        noSuchMethodErrorClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/NoSuchMethodError").load(), null);
        noSuchMethodErrorClass.postConstruct(this);
        noSuchFieldErrorClass = new VmThrowableClassImpl(this, bcc.findDefinedType("java/lang/NoSuchFieldError").load(), null);
        noSuchFieldErrorClass.postConstruct(this);

        stackTraceElementClass = new VmClassImpl(this, bcc.findDefinedType("java/lang/StackTraceElement").load(), null);
        stackTraceElementClass.postConstruct(this);

        memberNameClass = new VmMemberNameClassImpl(this, bcc.findDefinedType("java/lang/invoke/MemberName").load(), null);
        memberNameClass.postConstruct(this);

        // set up the bootstrap class loader *last*
        bootstrapClassLoader = new VmClassLoaderImpl(classLoaderClass, this);

        // now register the classes
        bootstrapClassLoader.registerClass("java/lang/Object", objectClass);
        bootstrapClassLoader.registerClass("java/lang/Class", classClass);
        bootstrapClassLoader.registerClass("java/lang/String", stringClass);
        bootstrapClassLoader.registerClass("java/lang/Thread", threadClass);
        bootstrapClassLoader.registerClass("java/lang/Throwable", throwableClass);
        bootstrapClassLoader.registerClass("java/lang/ClassLoader", classLoaderClass);

        bootstrapClassLoader.registerClass("java/lang/Error", errorClass);
        bootstrapClassLoader.registerClass("java/lang/LinkageError", linkageErrorClass);
        bootstrapClassLoader.registerClass("java/lang/IncompatibleClassChangeError", incompatibleClassChangeErrorClass);
        bootstrapClassLoader.registerClass("java/lang/NoClassDefFoundError", noClassDefFoundErrorClass);
        bootstrapClassLoader.registerClass("java/lang/NoSuchMethodError", noSuchMethodErrorClass);

        bootstrapClassLoader.registerClass("java/lang/StackTraceElement", stackTraceElementClass);

        bootstrapClassLoader.registerClass("java/lang/invoke/MemberName", memberNameClass);

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

            // VMHelpers
            VmClassImpl vmHelpersClass = bootstrapClassLoader.loadClass("org/qbicc/runtime/main/VMHelpers");

            vmHelpersClass.registerInvokable("get_class", (thread, target, args) -> ((VmObjectImpl) args.get(0)).getVmClass());
            vmHelpersClass.registerInvokable("classForName", (thread, target, args) -> {
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

            // ObjectModel
            VmClassImpl objectModelClass = bootstrapClassLoader.loadClass("org/qbicc/runtime/main/ObjectModel");

            objectModelClass.registerInvokable("type_id_of", (thread, target, args) -> ((VmObjectImpl) args.get(0)).getObjectTypeId());
            objectModelClass.registerInvokable("get_type_id_from_class", (thread, target, args) -> ((VmClassImpl) args.get(0)).getInstanceObjectTypeId());

            // Unsafe
            VmClassImpl unsafeClass = bootstrapClassLoader.loadClass("jdk/internal/misc/Unsafe");

            unsafeClass.registerInvokable("ensureClassInitialized", (thread, target, args) -> {
                ((VmClassImpl) args.get(0)).initialize((VmThreadImpl) thread);
                return null;
            });
            unsafeClass.registerInvokable("shouldBeInitialized0", (thread, target, args) ->
                Boolean.valueOf(((VmClassImpl) args.get(0)).shouldBeInitialized()));

            // System
            VmClassImpl systemClass = bootstrapClassLoader.loadClass("java/lang/System");

            systemClass.registerInvokable("nanoTime", (thread, target, args) -> Long.valueOf(System.nanoTime()));
            systemClass.registerInvokable("currentTimeMillis", (thread, target, args) -> Long.valueOf(System.currentTimeMillis()));
            systemClass.registerInvokable("initProperties", this::initProperties);

            //    private static native void initStackTraceElements(StackTraceElement[] elements,
            //                                                      Throwable x);

            // StackTraceElement
            VmClassImpl stackTraceElementClass = bootstrapClassLoader.loadClass("java/lang/StackTraceElement");

            stackTraceElementClass.registerInvokable("initStackTraceElements", (thread, target, args) -> {
                VmArrayImpl stackTrace = (VmArrayImpl) args.get(0);
                VmThrowableImpl throwable = (VmThrowableImpl) args.get(1);
                throwable.initStackTraceElements(stackTrace);
                return null;
            });

            // String
            VmClassImpl stringClass = bootstrapClassLoader.loadClass("java/lang/String");

            stringClass.registerInvokable("intern", (thread, target, args) -> intern((VmStringImpl) target));
            // for performance:
            // String.hashCode is well-defined
            stringClass.registerInvokable("hashCode", (thread, target, args) -> Integer.valueOf(((VmStringImpl) target).getContent().hashCode()));
            stringClass.registerInvokable("equals", (thread, target, args) -> Boolean.valueOf(args.get(0) instanceof VmStringImpl other && ((VmStringImpl)target).contentEquals(other.getContent())));
            stringClass.registerInvokable("coder", (thread, target, args) -> Byte.valueOf((byte) ((VmStringImpl) target).getMemory().load8(stringCoderOffset, MemoryAtomicityMode.UNORDERED)));
            stringClass.registerInvokable("isLatin1", (thread, target, args) -> Boolean.valueOf(((VmStringImpl) target).getMemory().load8(stringCoderOffset, MemoryAtomicityMode.UNORDERED) == 0));
            stringClass.registerInvokable("length", (thread, target, args) -> Integer.valueOf(((VmStringImpl) target).getContent().length()));
            stringClass.registerInvokable("charAt", (thread, target, args) -> Integer.valueOf(((VmStringImpl) target).getContent().charAt(((Integer) args.get(0)).intValue())));

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
            classClass.registerInvokable("getDeclaredFields0", (thread, target, args) -> {
                boolean publicOnly = ((Boolean)args.get(0)).booleanValue();
                return ((VmClass) target).getDeclaredFields(publicOnly);
            });
            classClass.registerInvokable("getModifiers", (thread, target, args) -> ((VmClass)target).getTypeDefinition().getModifiers());
            classClass.registerInvokable("getSuperclass", (thread, target, args) -> {
                LoadedTypeDefinition sc = ((VmClass)target).getTypeDefinition().getSuperClass();
                return sc == null ? null : sc.getVmClass();
            });
            classClass.registerInvokable("isArray", (thread, target, args) -> {
                VmClassImpl clazz = (VmClassImpl) target;
                return clazz instanceof VmArrayClass;
            });
            classClass.registerInvokable("isInterface", (thread, target, args) ->
                Boolean.valueOf(((VmClassImpl) target).getTypeDefinition().isInterface()));
            classClass.registerInvokable("isAssignableFrom", (thread, target, args) -> {
                VmClassImpl lhs = (VmClassImpl) target;
                VmClassImpl rhs = (VmClassImpl)args.get(0);
                return rhs.getTypeDefinition().isSubtypeOf(lhs.getTypeDefinition());
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
                VmArrayClassImpl arrayClass = vm.objectClass.getArrayClass();
                VmArrayImpl vmArray = arrayClass.newInstance(3);
                ClassContext emcCtxt = emcDef.getContext();
                VmClassLoaderImpl emcLoader = vm.getClassLoaderForContext(emcCtxt);
                VmClassImpl emc = emcLoader.loadClass(emcDef.getInternalName());
                vmArray.getMemory().storeRef(0, vm.intern(emc.getName()), MemoryAtomicityMode.UNORDERED);
                MethodElement enclosingMethod = def.getEnclosingMethod();
                if (enclosingMethod != null) {
                    vmArray.getMemory().storeRef(1, vm.intern(enclosingMethod.getName()), MemoryAtomicityMode.UNORDERED);
                    vmArray.getMemory().storeRef(2, vm.intern(enclosingMethod.getDescriptor().toString()), MemoryAtomicityMode.UNORDERED);
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
                while (enclosing.element.getEnclosingType().getInternalName().equals("java/lang/reflect/Method")) {
                    enclosing = enclosing.enclosing;
                }
                DefinedTypeDefinition def = enclosing.element.getEnclosingType();
                VmClassLoaderImpl cl = ((VmThreadImpl)thread).vm.getClassLoaderForContext(def.getContext());
                VmClassImpl clazz = cl.loadClass(def.getInternalName());
                return clazz;
            });
            reflectClass.registerInvokable("getClassAccessFlags", (thread, target, args) -> {
                VmClassImpl cls = (VmClassImpl)args.get(0);
                return cls.getTypeDefinition().getModifiers() & 0x1FFF;
            });

            // Signal
            VmClassImpl signalClass = bootstrapClassLoader.loadClass("jdk/internal/misc/Signal");
            signalClass.registerInvokable("findSignal0", (thread, target, args) -> {
                VmString sigName = (VmString) args.get(0);
                if (sigName.contentEquals("INT")) {
                    return 2;
                } else if (sigName.contentEquals("TERM")) {
                    return 15;
                } else {
                    return -1;
                }
            });

            // OSEnvironment
            VmClassImpl osEnvClass = bootstrapClassLoader.loadClass("jdk/internal/misc/OSEnvironment");
            osEnvClass.registerInvokable("initialize", (thread, target, args) -> null); // Skip this for build-time init.

            VmClassImpl unixDispatcher = bootstrapClassLoader.loadClass("sun/nio/fs/UnixNativeDispatcher");
            unixDispatcher.registerInvokable("getcwd", (thread, target, args) -> {
                byte[] cwd = System.getProperty("user.dir").getBytes();
                VmByteArrayImpl bytes = manuallyInitialize(byteArrayClass.newInstance(cwd.length));
                for (int i=0; i<cwd.length; i++) {
                    bytes.getMemory().store8(bytes.getArrayElementOffset(i), cwd[i], MemoryAtomicityMode.UNORDERED);
                }
                return bytes;
            });

            // MethodHandleNatives
            VmClassImpl methodHandleNatives = bootstrapClassLoader.loadClass("java/lang/invoke/MethodHandleNatives");
            methodHandleNatives.registerInvokable("resolve", (thread, target, args) -> {
                VmThreadImpl ourThread = (VmThreadImpl) thread;
                VmMemberNameImpl self = (VmMemberNameImpl) args.get(0);
                VmClassImpl caller = (VmClassImpl) args.get(1);
                boolean speculativeResolve = ((Boolean) args.get(2)).booleanValue();
                self.resolve(ourThread, caller, speculativeResolve);
                return self;
            });

            // Now execute system initialization
            LoadedTypeDefinition systemType = systemClass.getTypeDefinition();
            // phase 1
            invokeExact(systemType.getMethod(systemType.findSingleMethodIndex(me -> me.nameEquals("initPhase1"))), null, List.of());
            // phase 2
            // TODO: Not working yet; gets part way through and crashes.
            // invokeExact(systemType.getMethod(systemType.findSingleMethodIndex(me -> me.nameEquals("initPhase2"))), null, List.of(false, false));
            // phase 3
            // TODO: Haven't tried yet...still working on phase2
            // invokeExact(systemType.getMethod(systemType.findSingleMethodIndex(me -> me.nameEquals("initPhase3"))), null, List.of());

            // Initialize early to avoid deadlocks
            initialize(bootstrapClassLoader.loadClass("java/lang/ref/Reference"));
        }
    }

    private Object initProperties(final VmThread thread, final VmObject target, final List<Object> args) {
        VmObjectImpl props = (VmObjectImpl) args.get(0);
        URL propsResource = VmImpl.class.getClassLoader().getResource("system.properties");
        Properties properties = new Properties();
        if (propsResource != null) {
            try (InputStream is = propsResource.openStream()) {
                try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    try (BufferedReader br = new BufferedReader(isr)) {
                        properties.load(br);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Initial system properties could not be loaded");
            }
        } else {
            throw new IllegalStateException("Initial system properties could not be loaded");
        }
        // qbicc global fixed properties
        for (String name : properties.stringPropertyNames()) {
            setProperty(props, name, properties.getProperty(name));
        }
        // environment-specific properties
        Platform platform = ctxt.getPlatform();
        setProperty(props, "file.encoding", "UTF-8"); // only UTF-8
        setProperty(props, "file.separator", platform.getOs().getFileSeparator());

        // todo: java.class.path composed from command line
        setProperty(props, "java.home", System.getProperty("java.home")); // todo: spec must allow this to be undef
        setProperty(props, "java.io.tmpdir", System.getProperty("java.io.tmpdir")); // todo: reset at run time
        setProperty(props, "java.library.path", ""); // todo: JNI
        setProperty(props, "line.separator", platform.getOs().getLineSeparator());
        setProperty(props, "os.name", platform.getOs().getName());
        setProperty(props, "os.arch", platform.getCpu().getName());
        // todo: os.version
        setProperty(props, "path.separator", platform.getOs().getPathSeparator());

        setProperty(props, "user.country", Locale.getDefault().getCountry()); // todo: user-set locale on command line
        setProperty(props, "user.dir", System.getProperty("user.dir")); // todo: user.dir as a virtual directory, reset at run time
        setProperty(props, "user.home", System.getProperty("user.home")); // todo: user.home as a virtual directory, reset at run time
        setProperty(props, "user.language", Locale.getDefault().getLanguage()); // todo: user-set locale on command line
        setProperty(props, "user.name", System.getProperty("user.name")); // todo: user.name as temp user, reset at run time
        setProperty(props, "user.timezone", ""); // todo: reset at run time

        // these are non-spec but used by the JDK or other things
        setProperty(props, "sun.arch.data.model", String.valueOf(platform.getCpu().getCpuWordSize() << 3));
        // todo: sun.boot.library.path from command line
        setProperty(props, "sun.cpu.endian", ctxt.getTypeSystem().getEndianness() == ByteOrder.BIG_ENDIAN ? "big" : "little");
        setProperty(props, "sun.jnu.encoding", "UTF-8");

        return props;
    }

    void setProperty(VmObjectImpl properties, String key, String value) {
        invokeVirtual(setPropertyMethod, properties, List.of(intern(key), intern(value)));
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

    public VmByteArrayImpl allocateArray(byte[] bytes) {
        return manuallyInitialize(new VmByteArrayImpl(this, bytes));
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
        ((VmClassImpl)vmClass).initialize(vmThread);
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
        mtg.getMemory().storeRef(mtg.indexOf(sgDef.findField("name")), intern("system"), MemoryAtomicityMode.UNORDERED);
        mtg.getMemory().store32(mtg.indexOf(sgDef.findField("maxPriority")), 10 /* Thread.MAX_PRIORITY */, MemoryAtomicityMode.UNORDERED);
        return mtg;
    }

    @Override
    public MemoryImpl allocate(int size) {
        return emptyMemory.copy(size);
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
        for (int i = 0; i < size; i ++) {
            array.getMemory().storeRef(array.getArrayElementOffset(i), getClassForDescriptor(cl, parameterTypes.get(i)), MemoryAtomicityMode.UNORDERED);
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
        String nameStr;
        VmObject type;
        AnnotatedElement resolvedElement;
        VmClassImpl owner = getClassForDescriptor(cl, constant.getOwnerDescriptor());
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
            int mi = owner.getTypeDefinition().findConstructorIndex(descriptor);
            if (mi == -1) {
                throw new Thrown(noSuchMethodErrorClass.newInstance());
            }
            resolvedElement = owner.getTypeDefinition().getMethod(mi);
            extraFlags = 1 << 17;
        }
        VmStringImpl nameObj = intern(nameStr);
        // generate flags
        int kind = constant.getKind().getId();
        int modifiers = resolvedElement.getModifiers() & 0xffff; // only JVM-valid modifiers
        int flags = modifiers | extraFlags | (kind << 24);

        // instantiate the MethodName as fully resolved
        VmObject mn = manuallyInitialize(mnClass.newInstance());
        mn.getMemory().storeRef(mn.indexOf(mnDef.findField("clazz")), owner, MemoryAtomicityMode.UNORDERED);
        mn.getMemory().storeRef(mn.indexOf(mnDef.findField("name")), nameObj, MemoryAtomicityMode.UNORDERED);
        mn.getMemory().storeRef(mn.indexOf(mnDef.findField("type")), type, MemoryAtomicityMode.UNORDERED);
        mn.getMemory().store32(mn.indexOf(mnDef.findField("flags")), flags, MemoryAtomicityMode.UNORDERED);

        // call the factory method
        VmClassImpl dmhClass = bootstrapClassLoader.loadClass("java/lang/invoke/DirectMethodHandle");
        LoadedTypeDefinition dmhDef = dmhClass.getTypeDefinition();
        int makeIdx = dmhDef.findMethodIndex(me -> me.nameEquals("make") && me.getDescriptor().getParameterTypes().size() == 1);
        if (makeIdx == -1) {
            throw new IllegalStateException("No make() method found on DirectMethodHandle class");
        }
        return (VmObject) invokeExact(dmhDef.getMethod(makeIdx), null, List.of(mn));
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
            byte[] values = ((ByteArrayLiteral) literal).getValues();
            int length = values.length;
            VmByteArrayImpl vmByteArray = byteArrayClass.newInstance(length);
            vmByteArray.getMemory().storeMemory(vmByteArray.getArrayElementOffset(0), values, 0, length);
            return vmByteArray;
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
        } else {
            throw new UnsupportedOperationException("Boxing literal of type " + literal.getClass());
        }
    }

    @Override
    public VmReferenceArray newArrayOf(VmClass elementType, int size) {
        if (elementType instanceof VmPrimitiveClass) {
            throw new IllegalArgumentException("Cannot create a reference array with a primitive element type");
        }
        return (VmReferenceArray) ((VmClassImpl)elementType).getArrayClass().newInstance(size);
    }

    @Override
    public VmObject getLookup(VmClass vmClass) {
        // todo: cache these on VmClassImpl?
        VmClassImpl lookupClass = bootstrapClassLoader.loadClass("java/lang/invoke/MethodHandles$Lookup");
        LoadedTypeDefinition lookupDef = lookupClass.getTypeDefinition();
        VmObjectImpl lookupObj = manuallyInitialize(lookupClass.newInstance());
        lookupObj.getMemory().storeRef(lookupObj.indexOf(lookupDef.findField("lookupClass")), vmClass, MemoryAtomicityMode.UNORDERED);
        int fullPower = MethodHandles.Lookup.PUBLIC |
            MethodHandles.Lookup.PRIVATE |
            MethodHandles.Lookup.PROTECTED |
            MethodHandles.Lookup.PACKAGE |
            MethodHandles.Lookup.MODULE;
        lookupObj.getMemory().store32(lookupObj.indexOf(lookupDef.findField("allowedModes")), fullPower, MemoryAtomicityMode.UNORDERED);
        VarHandle.releaseFence();
        return lookupObj;
    }

    @Override
    public VmThread[] getStartedThreads() {
        return startedThreads.toArray(VmThread[]::new);
    }

    VmClassImpl getClassForDescriptor(VmClassLoaderImpl cl, TypeDescriptor descriptor) {
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
        return intern(vmString.getContent());
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

    public static VmImpl create(CompilationContext ctxt, Consumer<VmObject> manualInitializer) {
        return new VmImpl(Assert.checkNotNullParam("ctxt", ctxt), manualInitializer);
    }

    static VmImpl require() {
        return (VmImpl) Vm.requireCurrent();
    }

    private VmInvokable getInstanceInvoker(ExecutableElement element) {
        DefinedTypeDefinition enclosingType = element.getEnclosingType();
        VmClassLoaderImpl classLoader = getClassLoaderForContext(enclosingType.getContext());
        VmClassImpl loadedClass = classLoader.loadClass(enclosingType.getInternalName());
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
        MemoryImpl memory = globals.get(variableElement);
        if (memory == null) {
            memory = allocate((int) variableElement.getType().getSize());
            MemoryImpl appearing = globals.putIfAbsent(variableElement, memory);
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

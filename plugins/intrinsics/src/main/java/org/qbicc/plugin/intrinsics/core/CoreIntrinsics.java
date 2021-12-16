package org.qbicc.plugin.intrinsics.core;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Driver;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.GlobalVariable;
import org.qbicc.graph.Load;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.Store;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.Variable;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.machine.probe.CProbe;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.plugin.gc.nogc.NoGc;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayTables;
import org.qbicc.plugin.intrinsics.InstanceIntrinsic;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.plugin.intrinsics.StaticIntrinsic;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.methodinfo.MethodDataTypes;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.CompoundType.Member;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.NullableType;
import org.qbicc.type.PointerType;
import org.qbicc.type.Primitive;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 * Core JDK intrinsics.
 */
public final class CoreIntrinsics {
    public static final Logger log = Logger.getLogger("org.qbicc.plugin.intrinsics");

    public static void register(CompilationContext ctxt) {
        CNativeIntrinsics.register(ctxt);
        registerEmptyNativeInitMethods(ctxt);
        registerJavaIoFileDescriptorIntrinsics(ctxt);
        registerJavaLangClassIntrinsics(ctxt);
        registerJavaLangInvokeMethodHandleNativesIntrinsics(ctxt);
        registerJavaLangInvokeMethodHandleIntrinsics(ctxt);
        registerJavaLangStringIntrinsics(ctxt);
        registerJavaLangStringUTF16Intrinsics(ctxt);
        registerJavaLangSystemIntrinsics(ctxt);
        registerJavaLangStackTraceElementInstrinsics(ctxt);
        registerJavaLangThreadIntrinsics(ctxt);
        registerJavaLangThrowableIntrinsics(ctxt);
        registerJavaLangObjectIntrinsics(ctxt);
        registerJavaLangNumberIntrinsics(ctxt);
        registerJavaLangFloatDoubleMathIntrinsics(ctxt);
        registerJavaLangRefIntrinsics(ctxt);
        registerJavaLangRuntimeIntrinsics(ctxt);
        registerOrgQbiccCompilerIntrinsics(ctxt);
        registerOrgQbiccObjectModelIntrinsics(ctxt);
        registerOrgQbiccRuntimeMainIntrinsics(ctxt);
        registerJavaLangMathIntrinsics(ctxt);
        registerJavaUtilConcurrentAtomicLongIntrinsics(ctxt);
        registerOrgQbiccRuntimePosixPthreadCastPtr(ctxt);
        registerOrgQbiccRuntimeMethodDataIntrinsics(ctxt);
        UnsafeIntrinsics.register(ctxt);
        registerJDKInternalIntrinsics(ctxt);
    }

    private static void registerEmptyNativeInitMethods(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        Literal voidLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());
        Literal falseLiteral = ctxt.getLiteralFactory().literalOf(false);

        StaticIntrinsic emptyInit = (builder, target, arguments) -> voidLiteral;
        InstanceIntrinsic alwaysFalse = (builder, instance, target, arguments) -> falseLiteral;

        ClassTypeDescriptor fileInputStreamDesc = ClassTypeDescriptor.synthesize(classContext, "java/io/FileInputStream");
        ClassTypeDescriptor fileOutputStreamDesc = ClassTypeDescriptor.synthesize(classContext, "java/io/FileOutputStream");
        ClassTypeDescriptor fileDescriptorDesc = ClassTypeDescriptor.synthesize(classContext, "java/io/FileDescriptor");
        ClassTypeDescriptor randomAccessFileDesc = ClassTypeDescriptor.synthesize(classContext, "java/io/RandomAccessFile");
        ClassTypeDescriptor unixFileSystemDesc = ClassTypeDescriptor.synthesize(classContext, "java/io/UnixFileSystem");
        ClassTypeDescriptor winNtFileSystem = ClassTypeDescriptor.synthesize(classContext, "java/io/WinNTFileSystem");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor classLoaderDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ClassLoader");
        ClassTypeDescriptor threadDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Thread");
        ClassTypeDescriptor systemDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/System");
        ClassTypeDescriptor methodHandleNativesDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/invoke/MethodHandleNatives");
        ClassTypeDescriptor i4aDesc = ClassTypeDescriptor.synthesize(classContext, "java/net/Inet4Address");
        ClassTypeDescriptor i6aDesc = ClassTypeDescriptor.synthesize(classContext, "java/net/Inet6Address");
        ClassTypeDescriptor iaDesc = ClassTypeDescriptor.synthesize(classContext, "java/net/InetAddress");
        ClassTypeDescriptor networkInterfaceDesc = ClassTypeDescriptor.synthesize(classContext, "java/net/NetworkInterface");
        ClassTypeDescriptor inflateDesc = ClassTypeDescriptor.synthesize(classContext, "java/util/zip/Inflater");
        ClassTypeDescriptor scopedMemoryAccessDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/ScopedMemoryAccess");
        ClassTypeDescriptor unsafeDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/Unsafe");
        ClassTypeDescriptor vmDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/VM");
        ClassTypeDescriptor perfDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/perf/Perf");
        ClassTypeDescriptor aixNativeDispatcherDesc = ClassTypeDescriptor.synthesize(classContext, "sun/nio/fs/AixNativeDispatcher");
        ClassTypeDescriptor bsdNativeDispatcherDesc = ClassTypeDescriptor.synthesize(classContext, "sun/nio/fs/BsdNativeDispatcher");
        ClassTypeDescriptor linuxNativeDispatcherDesc = ClassTypeDescriptor.synthesize(classContext, "sun/nio/fs/LinuxNativeDispatcher");
        ClassTypeDescriptor solarisNativeDispatcherDesc = ClassTypeDescriptor.synthesize(classContext, "sun/nio/fs/SolarisNativeDispatcher");
        ClassTypeDescriptor windowsNativeDispatcherDesc = ClassTypeDescriptor.synthesize(classContext, "sun/nio/fs/WindowsNativeDispatcher");

        MethodDescriptor emptyToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());
        MethodDescriptor classToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(classDesc));
        MethodDescriptor classToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(classDesc));

        intrinsics.registerIntrinsic(fileInputStreamDesc, "initIDs", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(fileOutputStreamDesc, "initIDs", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(fileDescriptorDesc, "initIDs", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(randomAccessFileDesc, "initIDs", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(unixFileSystemDesc, "initIDs", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(winNtFileSystem, "initIDs", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(classDesc, "registerNatives", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(classLoaderDesc, "registerNatives", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(threadDesc, "registerNatives", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(systemDesc, "registerNatives", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(methodHandleNativesDesc, "registerNatives", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(i4aDesc, "init", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(i6aDesc, "init", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(iaDesc, "init", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(networkInterfaceDesc, "init", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(inflateDesc, "initIDs", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(scopedMemoryAccessDesc, "registerNatives", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(unsafeDesc, "registerNatives", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(Phase.ANALYZE, unsafeDesc, "ensureClassInitialized", classToVoid, emptyInit);
        intrinsics.registerIntrinsic(Phase.ANALYZE, unsafeDesc, "shouldBeInitialized0", classToBool, alwaysFalse);
        intrinsics.registerIntrinsic(vmDesc, "initialize", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(vmDesc, "initializeFromArchive", classToVoid, emptyInit);
        intrinsics.registerIntrinsic(perfDesc, "registerNatives", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(aixNativeDispatcherDesc, "init", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(bsdNativeDispatcherDesc, "initIDs", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(linuxNativeDispatcherDesc, "init", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(solarisNativeDispatcherDesc, "init", emptyToVoid, emptyInit);
        intrinsics.registerIntrinsic(windowsNativeDispatcherDesc, "initIDs", emptyToVoid, emptyInit);
    }

    private static StaticIntrinsic setVolatile(CompilationContext ctxt, FieldElement field) {
        Literal voidLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());
        return (builder, target, arguments) -> {
            builder.store(builder.staticField(field), arguments.get(0), MemoryAtomicityMode.VOLATILE);
            return voidLiteral;
        };
    }

    private static void registerJavaIoFileDescriptorIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor fileDescriptorDesc = ClassTypeDescriptor.synthesize(classContext, "java/io/FileDescriptor");

        MethodDescriptor intToLong = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.I));
        MethodDescriptor intToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.I));

        StaticIntrinsic getHandle = (builder, target, arguments) -> {
            return ctxt.getLiteralFactory().literalOf(-1L); // TODO: real implementation (for Windows)
        };

        StaticIntrinsic getAppend = (builder, target, arguments) -> {
            return ctxt.getLiteralFactory().literalOf(false); // TODO: real implementation
        };

        intrinsics.registerIntrinsic(fileDescriptorDesc, "getHandle", intToLong, getHandle);
        intrinsics.registerIntrinsic(fileDescriptorDesc, "getAppend", intToBool, getAppend);
    }

    public static void registerJavaLangClassIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        CoreClasses coreClasses = CoreClasses.get(ctxt);
        SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
        RuntimeMethodFinder methodFinder = RuntimeMethodFinder.get(ctxt);

        ClassTypeDescriptor jlcDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor jlclDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ClassLoader");
        ClassTypeDescriptor jlsDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor jloDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");

        MethodDescriptor classToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(jlcDesc));
        MethodDescriptor emptyToObjArray = MethodDescriptor.synthesize(classContext, ArrayTypeDescriptor.of(classContext, jloDesc), List.of());
        MethodDescriptor emptyToClass = MethodDescriptor.synthesize(classContext, jlcDesc, List.of());
        MethodDescriptor emptyToString = MethodDescriptor.synthesize(classContext, jlsDesc, List.of());
        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());
        MethodDescriptor stringToClass = MethodDescriptor.synthesize(classContext, jlcDesc, List.of(jlsDesc));
        MethodDescriptor objToObj = MethodDescriptor.synthesize(classContext, jloDesc, List.of(jloDesc));
        MethodDescriptor stringBoolLoaderClassToClass = MethodDescriptor.synthesize(classContext, jlcDesc, List.of(jlsDesc, BaseTypeDescriptor.Z, jlclDesc, jlcDesc));

        // Assertion status

        // todo: this probably belongs in the class libraries rather than here
        StaticIntrinsic desiredAssertionStatus0 = (builder, target, arguments) ->
            classContext.getLiteralFactory().literalOf(false);

        InstanceIntrinsic desiredAssertionStatus =  (builder, instance, target, arguments) ->
            classContext.getLiteralFactory().literalOf(false);

        InstanceIntrinsic initClassName = (builder, instance, target, arguments) -> {
            // not reachable; we always would initialize our class name eagerly
            throw new BlockEarlyTermination(builder.unreachable());
        };

        InstanceIntrinsic isArray = (builder, instance, target, arguments) -> {
            Value id = builder.load(builder.instanceFieldOf(builder.referenceHandle(instance),  coreClasses.getClassTypeIdField()), MemoryAtomicityMode.UNORDERED);
            Value dims = builder.load(builder.instanceFieldOf(builder.referenceHandle(instance),  coreClasses.getClassDimensionField()), MemoryAtomicityMode.UNORDERED);
            MethodElement isPrimArray = methodFinder.getMethod("isPrimArray");
            return builder.or(builder.getFirstBuilder().isGt(dims, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(dims.getType())),
                              builder.getFirstBuilder().call(builder.staticMethod(isPrimArray, isPrimArray.getDescriptor(), isPrimArray.getType()), List.of(id)));
        };

        InstanceIntrinsic isInterface = (builder, instance, target, arguments) -> {
            Value id = builder.load(builder.instanceFieldOf(builder.referenceHandle(instance),  coreClasses.getClassTypeIdField()), MemoryAtomicityMode.UNORDERED);
            MethodElement isIntf = methodFinder.getMethod("isInterface");
            return builder.getFirstBuilder().call(builder.staticMethod(isIntf, isIntf.getDescriptor(), isIntf.getType()), List.of(id));
        };

        InstanceIntrinsic isPrimitive = (builder, instance, target, arguments) -> {
            Value id = builder.load(builder.instanceFieldOf(builder.referenceHandle(instance), coreClasses.getClassTypeIdField()), MemoryAtomicityMode.UNORDERED);
            MethodElement isPrim = methodFinder.getMethod("isPrimitive");
            return builder.getFirstBuilder().call(builder.staticMethod(isPrim, isPrim.getDescriptor(), isPrim.getType()), List.of(id));
        };

        InstanceIntrinsic getSuperclass = (builder, instance, target, arguments) -> {
            Value id = builder.load(builder.instanceFieldOf(builder.referenceHandle(instance), coreClasses.getClassTypeIdField()), MemoryAtomicityMode.UNORDERED);
            LiteralFactory lf = ctxt.getLiteralFactory();
            TypeSystem ts = ctxt.getTypeSystem();
            MethodElement helper = RuntimeMethodFinder.get(ctxt).getMethod("getSuperClass");
            return builder.getFirstBuilder().call(builder.staticMethod(helper, helper.getDescriptor(), helper.getType()), List.of(id));
        };

        StaticIntrinsic getPrimitiveClass = (builder, target, arguments) -> {
            // always called with a string literal
            StringLiteral lit = (StringLiteral) arguments.get(0);
            LiteralFactory lf = ctxt.getLiteralFactory();
            TypeSystem ts = ctxt.getTypeSystem();
            ValueType type = Primitive.getPrimitiveFor(lit.getValue()).getType();
            return builder.classOf(lf.literalOfType(type), lf.zeroInitializerLiteralOfType(ts.getUnsignedInteger8Type()));
        };

        //    static native Class<?> getPrimitiveClass(String name);

        intrinsics.registerIntrinsic(jlcDesc, "desiredAssertionStatus0", classToBool, desiredAssertionStatus0);
        intrinsics.registerIntrinsic(jlcDesc, "desiredAssertionStatus", emptyToBool, desiredAssertionStatus);
        intrinsics.registerIntrinsic(jlcDesc, "initClassName", emptyToString, initClassName);
        intrinsics.registerIntrinsic(jlcDesc, "getPrimitiveClass", stringToClass, getPrimitiveClass);
        intrinsics.registerIntrinsic(Phase.LOWER, jlcDesc, "getSuperclass", emptyToClass, getSuperclass);
        intrinsics.registerIntrinsic(Phase.LOWER, jlcDesc, "isArray", emptyToBool, isArray);
        intrinsics.registerIntrinsic(Phase.LOWER, jlcDesc, "isInterface", emptyToBool, isInterface);
        intrinsics.registerIntrinsic(Phase.LOWER, jlcDesc, "isPrimitive", emptyToBool, isPrimitive);

        StaticIntrinsic classForName0 = (builder, target, arguments) -> {
            // ignore fourth argument
            MethodElement vmhForName = RuntimeMethodFinder.get(ctxt).getMethod("classForName");
            return builder.call(builder.staticMethod(vmhForName, vmhForName.getDescriptor(), vmhForName.getType()), arguments.subList(0, 3));
        };

        intrinsics.registerIntrinsic(jlcDesc, "forName0", stringBoolLoaderClassToClass, classForName0);

        InstanceIntrinsic getEnclosingMethod0 = (builder, instance, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            return lf.nullLiteralOfType((NullableType) target.getExecutable().getType().getReturnType());
        };

        intrinsics.registerIntrinsic(Phase.ANALYZE, jlcDesc, "getEnclosingMethod0", emptyToObjArray, getEnclosingMethod0);

        InstanceIntrinsic getDeclaringClass0 = (builder, instance, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            return lf.nullLiteralOfType((NullableType) target.getExecutable().getType().getReturnType());
        };

        intrinsics.registerIntrinsic(Phase.ANALYZE, jlcDesc, "getDeclaringClass0", emptyToClass, getDeclaringClass0);
    }

    public static void registerJavaLangInvokeMethodHandleNativesIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        LiteralFactory lf = ctxt.getLiteralFactory();
        TypeSystem ts = ctxt.getTypeSystem();

        ClassTypeDescriptor memberNameDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/invoke/MemberName");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor jliMhnDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/invoke/MethodHandleNatives");

        MethodDescriptor emptyToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());
        MethodDescriptor memberNameClassBoolToMemberName = MethodDescriptor.synthesize(classContext,
            memberNameDesc,
            List.of(
                memberNameDesc,
                classDesc,
                BaseTypeDescriptor.Z
            )
        );

        StaticIntrinsic resolve = (builder, target, arguments) -> {
            LoadedTypeDefinition uoe = classContext.findDefinedType("java/lang/UnsupportedOperationException").load();
            ClassObjectType uoeType = uoe.getClassType();
            int idx = uoe.findConstructorIndex(emptyToVoid);
            if (idx == -1) {
                throw new IllegalStateException();
            }
            ConstructorElement ctor = uoe.getConstructor(idx);
            Layout layout = Layout.get(ctxt);
            CompoundType compoundType = layout.getInstanceLayoutInfo(uoe).getCompoundType();
            Value ex = builder.new_(uoeType, lf.literalOfType(uoeType), lf.literalOf(compoundType.getSize()), lf.literalOf(compoundType.getAlign()));
            builder.call(builder.constructorOf(ex, ctor, ctor.getDescriptor(), ctor.getType()), List.of());
            throw new BlockEarlyTermination(builder.throw_(ex));
        };

        intrinsics.registerIntrinsic(Phase.ANALYZE, jliMhnDesc, "resolve", memberNameClassBoolToMemberName, resolve);
    }

    public static void registerJavaLangInvokeMethodHandleIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        LiteralFactory lf = ctxt.getLiteralFactory();
        TypeSystem ts = ctxt.getTypeSystem();

        ClassTypeDescriptor methodHandleDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/invoke/MethodHandle");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor methodTypeDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/invoke/MethodType");
        ClassTypeDescriptor internalErrorDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/InternalError");
        ClassTypeDescriptor throwableDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Throwable");

        ArrayTypeDescriptor objArrayDesc = ArrayTypeDescriptor.of(classContext, objDesc);

        MethodDescriptor objArrayToObj = MethodDescriptor.synthesize(classContext, objDesc, List.of(objArrayDesc));
        MethodDescriptor throwableToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(throwableDesc));

        // mh.invoke(...) -> mh.asType(actualType).invokeExact(...)
        InstanceIntrinsic invoke = (builder, instance, target, arguments) -> {
            // Use first builder because we chain to other intrinsics!
            MethodDescriptor descriptor = target.getCallSiteDescriptor();
            Vm vm = Vm.requireCurrent();
            BasicBlockBuilder fb = builder.getFirstBuilder();
            try {
                VmObject realType = vm.createMethodType(classContext, descriptor);
                Value realHandle;
                LoadedTypeDefinition mhDef = ctxt.getBootstrapClassContext().findDefinedType("java/lang/invoke/MethodHandle").load();
                int asTypeIdx = mhDef.findMethodIndex(me -> me.nameEquals("asType"));
                MethodElement asType = mhDef.getMethod(asTypeIdx);
                if (instance instanceof ObjectLiteral) {
                    // get the target statically
                    realHandle = lf.literalOf((VmObject) vm.invokeExact(asType, ((ObjectLiteral) instance).getValue(), List.of(realType)));
                } else {
                    // get the target dynamically
                    ValueHandle asTypeHandle = fb.exactMethodOf(instance, asType, asType.getDescriptor(), asType.getType());
                    realHandle = fb.call(asTypeHandle, List.of(lf.literalOf(realType)));
                }
                ValueHandle invokeExactHandle = fb.exactMethodOf(realHandle, methodHandleDesc, "invokeExact", descriptor);
                return fb.call(invokeExactHandle, arguments);
            } catch (Thrown t) {
                ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.invoke intrinsic: %s", t);
                log.warnf(t, "Failed to expand MethodHandle.invoke intrinsic");
                Value ie = fb.new_(internalErrorDesc);
                fb.call(fb.constructorOf(ie, internalErrorDesc, throwableToVoid), List.of(lf.literalOf(t.getThrowable())));
                throw new BlockEarlyTermination(fb.throw_(ie));
            }
        };
        // this intrinsic MUST be added during ADD because `invoke` must always be converted.
        intrinsics.registerIntrinsic(Phase.ADD, methodHandleDesc, "invoke", objArrayToObj, invoke);
    }

    public static void registerJavaLangStringIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor stringDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");

        MethodDescriptor emptyToString = MethodDescriptor.synthesize(classContext, stringDesc, List.of());

        InstanceIntrinsic intern = (builder, instance, target, arguments) ->
            // todo: implement a proper interning table, intercept in interpreter
            instance;

        intrinsics.registerIntrinsic(stringDesc, "intern", emptyToString, intern);
    }

    public static void registerJavaLangStringUTF16Intrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jlsu16Desc = ClassTypeDescriptor.synthesize(classContext, "java/lang/StringUTF16");

        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        //    private static native boolean isBigEndian();

        CProbe probe = CProbe.builder().build();
        try {
            CProbe.Result result = probe.run(ctxt.getAttachment(Driver.C_TOOL_CHAIN_KEY), ctxt.getAttachment(Driver.OBJ_PROVIDER_TOOL_KEY), ctxt);
            if (result == null) {
                ctxt.error("Failed to probe target endianness (no exception)");
            } else {
                StaticIntrinsic isBigEndian = (builder, target, arguments) ->
                    ctxt.getLiteralFactory().literalOf(result.getByteOrder() == ByteOrder.BIG_ENDIAN);

                intrinsics.registerIntrinsic(jlsu16Desc, "isBigEndian", emptyToBool, isBigEndian);
            }
        } catch (IOException e) {
            ctxt.error(e, "Failed to probe target endianness");
        }
    }

    public static void registerJavaLangSystemIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor systemDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/System");
        LoadedTypeDefinition jls = classContext.findDefinedType("java/lang/System").load();
        ClassTypeDescriptor jloDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor stringDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor vmDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/VM");

        MethodDescriptor objectToIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(jloDesc));

        // Null and no-operation intrinsics

        StaticIntrinsic returnNull = (builder, target, arguments) ->
            classContext.getLiteralFactory().zeroInitializerLiteralOfType(jls.getClassType().getReference());
        intrinsics.registerIntrinsic(systemDesc, "getSecurityManager",
            MethodDescriptor.synthesize(classContext,
                ClassTypeDescriptor.synthesize(classContext,"java/lang/SecurityManager"), List.of()),
            returnNull);

        // System public API

        FieldElement in = jls.findField("in");
        in.setModifierFlags(ClassFile.I_ACC_NOT_REALLY_FINAL);
        FieldElement out = jls.findField("out");
        out.setModifierFlags(ClassFile.I_ACC_NOT_REALLY_FINAL);
        FieldElement err = jls.findField("err");
        err.setModifierFlags(ClassFile.I_ACC_NOT_REALLY_FINAL);

        // Setters

        MethodDescriptor setInputStreamDesc =
            MethodDescriptor.synthesize(classContext,
                BaseTypeDescriptor.V, List.of(ClassTypeDescriptor.synthesize(classContext, "java/io/InputStream")));
        MethodDescriptor setPrintStreamDesc =
            MethodDescriptor.synthesize(classContext,
                BaseTypeDescriptor.V, List.of(ClassTypeDescriptor.synthesize(classContext, "java/io/PrintStream")));

        intrinsics.registerIntrinsic(systemDesc, "setIn0", setInputStreamDesc, setVolatile(ctxt, in));
        intrinsics.registerIntrinsic(systemDesc, "setOut0", setPrintStreamDesc, setVolatile(ctxt, out));
        intrinsics.registerIntrinsic(systemDesc, "setErr0", setPrintStreamDesc, setVolatile(ctxt, err));

        // arraycopy

        MethodDescriptor arraycopyDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(
            jloDesc,
            BaseTypeDescriptor.I,
            jloDesc,
            BaseTypeDescriptor.I,
            BaseTypeDescriptor.I
        ));

        StaticIntrinsic arraycopy = (builder, target, arguments) ->
            builder.call(builder.staticMethod(vmDesc, "arraycopy", target.getExecutable().getDescriptor()), arguments);

        intrinsics.registerIntrinsic(systemDesc, "arraycopy", arraycopyDesc, arraycopy);

        // identity hash code

        // todo: obviously non-optimal; replace once we have object headers sorted out
        StaticIntrinsic identityHashCode = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(0);

        intrinsics.registerIntrinsic(systemDesc, "identityHashCode", objectToIntDesc, identityHashCode);

        MethodDescriptor stringToVoidDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(stringDesc));

        StaticIntrinsic loadLibrary = (builder, target, arguments) -> {
            Value libraryName = arguments.get(0);
            String content;
            if (libraryName instanceof StringLiteral) {
                content = ((StringLiteral) libraryName).getValue();
            } else if (libraryName instanceof ObjectLiteral) {
                VmObject value = ((ObjectLiteral) libraryName).getValue();
                if (value instanceof VmString) {
                    content = ((VmString) value).getContent();
                } else {
                    return null;
                }
            } else {
                return null;
            }
            if (content.equals("net") || content.equals("extnet") || content.equals("zip") || content.equals("nio") || content.equals("prefs")) {
                // ignore known libraries
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());
            } else {
                return null;
            }
        };

        intrinsics.registerIntrinsic(systemDesc, "loadLibrary", stringToVoidDesc, loadLibrary);
    }

    public static void registerJavaLangThreadIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        final int threadAlive = 0x0001;
        final int threadTerminated = 0x0002;
        final int threadRunnable = 0x0004;

        ClassTypeDescriptor jltDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Thread");
        ClassTypeDescriptor vmDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/VM");
        ClassTypeDescriptor vmHelpersDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/VMHelpers");
        ClassTypeDescriptor compIntrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/CompilerIntrinsics");
        ClassTypeDescriptor pthreadPtrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/posix/PThread$pthread_t_ptr");
        ClassTypeDescriptor voidPtrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$void_ptr");
        ClassTypeDescriptor voidUnaryfunctionPtrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$void_ptr_unaryoperator_function_ptr");

        MethodDescriptor returnJlt = MethodDescriptor.synthesize(classContext, jltDesc, List.of());
        MethodDescriptor voidDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());
        MethodDescriptor voidIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(BaseTypeDescriptor.I));
        MethodDescriptor booleanDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        StaticIntrinsic nopStatic = (builder, target, arguments) -> ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getExecutable().getType().getReturnType());
        InstanceIntrinsic nopInstance = (builder, instance, target, arguments) -> ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getExecutable().getType().getReturnType());

        /* registerNatives */
        intrinsics.registerIntrinsic(jltDesc, "registerNatives", voidDesc, nopStatic);

        /* public static native Thread currentThread(); */
        StaticIntrinsic currentThread = (builder, target, arguments) -> builder.load(builder.currentThread(), MemoryAtomicityMode.NONE);
        intrinsics.registerIntrinsic(jltDesc, "currentThread", returnJlt, currentThread);

        /* VMHelpers.java - threadWrapper: helper method for java.lang.Thread.start0 */
        MethodDescriptor threadWrapperNativeDesc = MethodDescriptor.synthesize(classContext, voidPtrDesc, List.of(voidPtrDesc));
        StaticIntrinsic threadWrapperNative = (builder, target, arguments) -> {
            Value threadVoidPtr = arguments.get(0);

            DefinedTypeDefinition jlt = classContext.findDefinedType("java/lang/Thread");
            LoadedTypeDefinition jltVal = jlt.load();
            ReferenceType jltType = jltVal.getType().getReference();
            Value threadObject = builder.valueConvert(threadVoidPtr, jltType);
            ValueHandle threadObjectHandle = builder.referenceHandle(threadObject);

            /* set current thread */
            builder.store(builder.staticField(vmDesc, "_qbicc_bound_thread", jltDesc), threadObject, MemoryAtomicityMode.NONE);

            /* call "run" method of thread object */
            VirtualMethodElementHandle runHandle = (VirtualMethodElementHandle)builder.virtualMethodOf(threadObject, jltDesc, "run", voidDesc);
            builder.call(runHandle, List.of());

            /* set java.lang.Thread.threadStatus to terminated */
            ValueHandle threadStatusHandle = builder.instanceFieldOf(threadObjectHandle, jltDesc, "threadStatus", BaseTypeDescriptor.I);
            builder.store(threadStatusHandle, ctxt.getLiteralFactory().literalOf(threadTerminated), MemoryAtomicityMode.NONE);

            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getExecutable().getType().getReturnType()); /* return null */
        };
        intrinsics.registerIntrinsic(compIntrDesc, "threadWrapperNative", threadWrapperNativeDesc, threadWrapperNative);

        /* VMHelpers.java - saveNativeThread: helper method for java.lang.Thread.start0 */
        MethodDescriptor saveNativeThreadDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(voidPtrDesc, pthreadPtrDesc));
        StaticIntrinsic saveNativeThread = (builder, target, arguments) -> {
            // TODO implement
            return ctxt.getLiteralFactory().literalOf(true);
        };
        intrinsics.registerIntrinsic(compIntrDesc, "saveNativeThread", saveNativeThreadDesc, saveNativeThread);

        /* private native void start0(); */
        InstanceIntrinsic start0 = (builder, instance, target, arguments) -> {
            PointerType voidPointerType = ctxt.getTypeSystem().getVoidType().getPointer();

            /* set java.lang.Thread.threadStatus to runnable and alive */
            ValueHandle threadStatusHandle = builder.instanceFieldOf(builder.referenceHandle(instance), jltDesc, "threadStatus", BaseTypeDescriptor.I);
            builder.store(threadStatusHandle, ctxt.getLiteralFactory().literalOf(threadRunnable | threadAlive), MemoryAtomicityMode.NONE);

            /* pass threadWrapper as function_ptr - TODO this will eventually be replaced by a call to CNative.addr_of_function */
            MethodDescriptor threadWrapperDesc = MethodDescriptor.synthesize(classContext, voidPtrDesc, List.of(voidPtrDesc));
            ValueHandle threadWrapperValueHandle = builder.staticMethod(vmHelpersDesc, "threadWrapper", threadWrapperDesc); // TODO: Once qbicc 0.3.0 comes out, change vmHelpersDesc to compIntrDesc
            Value threadWrapperFunctionPointer = builder.addressOf(threadWrapperValueHandle);

            /* call threadWrapper with null parameter so it does nothing - TODO this is a workaround to create a declares statement for threadWrapper in java.lang.Thread */
            builder.call(threadWrapperValueHandle, List.of(ctxt.getLiteralFactory().zeroInitializerLiteralOfType(voidPointerType)));

            /* pass java.lang.Thread object as ptr<void> */
            Value threadVoidPtr = builder.valueConvert(instance, voidPointerType);

            /* start pthread in VMHelpers */
            MethodDescriptor JLT_start0Desc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(voidUnaryfunctionPtrDesc, voidPtrDesc));
            builder.call(builder.staticMethod(vmHelpersDesc, "JLT_start0", JLT_start0Desc), List.of(threadWrapperFunctionPointer, threadVoidPtr));

            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());
        };
        intrinsics.registerIntrinsic(jltDesc, "start0", voidDesc, start0);

        /* public final native boolean isAlive(); */
        InstanceIntrinsic isAlive = (builder, instance, target, arguments) -> {
            ValueHandle threadStatusHandle = builder.instanceFieldOf(builder.referenceHandle(instance), jltDesc, "threadStatus", BaseTypeDescriptor.I);
            Value threadStatus = builder.load(threadStatusHandle, MemoryAtomicityMode.NONE);
            Value aliveState = ctxt.getLiteralFactory().literalOf(threadAlive);
            Value isThreadAlive = builder.and(threadStatus, aliveState);
            return builder.isEq(isThreadAlive, aliveState);
        };
        intrinsics.registerIntrinsic(jltDesc, "isAlive", booleanDesc, isAlive);

        /* private native void setPriority0(int newPriority); */
        intrinsics.registerIntrinsic(jltDesc, "setPriority0", voidIntDesc, nopInstance);
    }

    public static void registerJavaLangThrowableIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        RuntimeMethodFinder methodFinder = RuntimeMethodFinder.get(ctxt);

        String jsfcClass = "org/qbicc/runtime/stackwalk/JavaStackFrameCache";
        String jswClass = "org/qbicc/runtime/stackwalk/JavaStackWalker";

        ClassTypeDescriptor jltDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Throwable");

        MethodDescriptor intToVoidDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(BaseTypeDescriptor.I));

        MethodElement getFrameCountElement = methodFinder.getMethod(jswClass, "getFrameCount");
        MethodElement walkStackElement = methodFinder.getMethod(jswClass, "walkStack");

        MethodElement getSourceCodeIndexListElement = methodFinder.getMethod(jsfcClass, "getSourceCodeIndexList");
        ConstructorElement jsfcConstructor = methodFinder.getConstructor(jsfcClass, intToVoidDesc);

        InstanceIntrinsic fillInStackTrace = (builder, instance, target, arguments) -> {
            Value frameCount = builder.getFirstBuilder().call(
                builder.staticMethod(getFrameCountElement, getFrameCountElement.getDescriptor(), getFrameCountElement.getType()),
                List.of(instance));
            ClassObjectType jsfcClassType = (ClassObjectType) ctxt.getBootstrapClassContext().findDefinedType(jsfcClass).load().getType();
            CompoundType compoundType = Layout.get(ctxt).getInstanceLayoutInfo(jsfcClassType.getDefinition()).getCompoundType();
            LiteralFactory lf = ctxt.getLiteralFactory();
            Value visitor = builder.getFirstBuilder().new_(jsfcClassType, lf.literalOfType(jsfcClassType), lf.literalOf(compoundType.getSize()), lf.literalOf(compoundType.getAlign()));
            builder.call(
                builder.getFirstBuilder().constructorOf(visitor, jsfcConstructor, jsfcConstructor.getDescriptor(), jsfcConstructor.getType()),
                List.of(frameCount));
            builder.call(
                builder.getFirstBuilder().staticMethod(walkStackElement, walkStackElement.getDescriptor(), walkStackElement.getType()),
                List.of(instance, visitor));

            // set Throwable#backtrace and Throwable#depth fields
            DefinedTypeDefinition jlt = classContext.findDefinedType("java/lang/Throwable");
            LoadedTypeDefinition jltVal = jlt.load();
            FieldElement backtraceField = jltVal.findField("backtrace");
            FieldElement depthField = jltVal.findField("depth");
            Value backtraceValue = builder.getFirstBuilder().call(
                builder.virtualMethodOf(visitor, getSourceCodeIndexListElement, getSourceCodeIndexListElement.getDescriptor(), getSourceCodeIndexListElement.getType()),
                List.of());
            builder.store(builder.instanceFieldOf(builder.referenceHandle(instance), backtraceField), backtraceValue, MemoryAtomicityMode.NONE);
            builder.store(builder.instanceFieldOf(builder.referenceHandle(instance), depthField), frameCount, MemoryAtomicityMode.NONE);
            return instance;
        };

        intrinsics.registerIntrinsic(Phase.ANALYZE, jltDesc, "fillInStackTrace", MethodDescriptor.synthesize(classContext, jltDesc, List.of(BaseTypeDescriptor.I)), fillInStackTrace);
    }

    public static void registerJavaLangStackTraceElementInstrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        RuntimeMethodFinder methodFinder = RuntimeMethodFinder.get(ctxt);

        ClassTypeDescriptor steDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/StackTraceElement");
        ClassTypeDescriptor jltDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Throwable");
        ArrayTypeDescriptor steArrayDesc = ArrayTypeDescriptor.of(classContext, steDesc);
        MethodDescriptor steArrayThrowableToVoidDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(steArrayDesc, jltDesc));

        MethodElement fillStackTraceElements = methodFinder.getMethod("org/qbicc/runtime/stackwalk/MethodData", "fillStackTraceElements");

        StaticIntrinsic initStackTraceElements = (builder, target, arguments) -> {
            DefinedTypeDefinition jlt = classContext.findDefinedType("java/lang/Throwable");
            LoadedTypeDefinition jltVal = jlt.load();
            FieldElement backtraceField = jltVal.findField("backtrace");
            FieldElement depthField = jltVal.findField("depth");
            Value backtraceValue = builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(1)), backtraceField), MemoryAtomicityMode.NONE);
            Value depthValue = builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(1)), depthField), MemoryAtomicityMode.NONE);

            return builder.getFirstBuilder().call(builder.staticMethod(fillStackTraceElements, fillStackTraceElements.getDescriptor(), fillStackTraceElements.getType()), List.of(arguments.get(0), backtraceValue, depthValue));
        };

        intrinsics.registerIntrinsic(Phase.ANALYZE, steDesc, "initStackTraceElements", steArrayThrowableToVoidDesc, initStackTraceElements);
    }

    private static void registerOrgQbiccRuntimeMethodDataIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        RuntimeMethodFinder methodFinder = RuntimeMethodFinder.get(ctxt);

        ClassTypeDescriptor stringDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor mdDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stackwalk/MethodData");
        ClassTypeDescriptor jlsteDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/StackTraceElement");

        MethodDescriptor voidToIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of());
        MethodDescriptor intToLongDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.I));
        MethodDescriptor intToIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.I));
        MethodDescriptor intToStringDesc = MethodDescriptor.synthesize(classContext, stringDesc, List.of(BaseTypeDescriptor.I));
        MethodDescriptor steIntToVoidDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(jlsteDesc, BaseTypeDescriptor.I));

        MethodDataTypes mdTypes = MethodDataTypes.get(ctxt);
        CompoundType gmdType = mdTypes.getGlobalMethodDataType();
        CompoundType minfoType = mdTypes.getMethodInfoType();
        CompoundType scInfoType = mdTypes.getSourceCodeInfoType();

        StaticIntrinsic getInstructionListSize = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            return builder.load(builder.memberOf(gmdVariable, gmdType.getMember("instructionTableSize")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getInstructionListSize", voidToIntDesc, getInstructionListSize);

        StaticIntrinsic getInstructionAddress = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("instructionTable")), MemoryAtomicityMode.UNORDERED);
            return builder.load(builder.pointerHandle(tablePointer, arguments.get(0)), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getInstructionAddress", intToLongDesc, getInstructionAddress);

        StaticIntrinsic getSourceCodeInfoIndex = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("sourceCodeIndexTable")), MemoryAtomicityMode.UNORDERED);
            return builder.load(builder.pointerHandle(tablePointer, arguments.get(0)), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getSourceCodeInfoIndex", intToIntDesc, getSourceCodeInfoIndex);

        StaticIntrinsic getMethodInfoIndex = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("sourceCodeInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle scInfoHandle = builder.pointerHandle(builder.bitCast(tablePointer, scInfoType.getPointer()), arguments.get(0));
            return builder.load(builder.memberOf(scInfoHandle, scInfoType.getMember("methodInfoIndex")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getMethodInfoIndex", intToIntDesc, getMethodInfoIndex);

        StaticIntrinsic getLineNumber = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("sourceCodeInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle scInfoHandle = builder.pointerHandle(builder.bitCast(tablePointer, scInfoType.getPointer()), arguments.get(0));
            return builder.load(builder.memberOf(scInfoHandle, scInfoType.getMember("lineNumber")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getLineNumber", intToIntDesc, getLineNumber);

        StaticIntrinsic getBytecodeIndex = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("sourceCodeInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle scInfoHandle = builder.pointerHandle(builder.bitCast(tablePointer, scInfoType.getPointer()), arguments.get(0));
            return builder.load(builder.memberOf(scInfoHandle, scInfoType.getMember("bcIndex")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getBytecodeIndex", intToIntDesc, getBytecodeIndex);

        StaticIntrinsic getInlinedAtIndex = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("sourceCodeInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle scInfoHandle = builder.pointerHandle(builder.bitCast(tablePointer, scInfoType.getPointer()), arguments.get(0));
            return builder.load(builder.memberOf(scInfoHandle, scInfoType.getMember("inlinedAtIndex")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getInlinedAtIndex", intToIntDesc, getInlinedAtIndex);

        StaticIntrinsic getFileName = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("methodInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle minfoHandle = builder.pointerHandle(builder.bitCast(tablePointer, minfoType.getPointer()), arguments.get(0));
            return builder.load(builder.memberOf(minfoHandle, minfoType.getMember("fileName")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getFileName", intToStringDesc, getFileName);

        StaticIntrinsic getMethodName = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("methodInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle minfoHandle = builder.pointerHandle(builder.bitCast(tablePointer, minfoType.getPointer()), arguments.get(0));
            return builder.load(builder.memberOf(minfoHandle, minfoType.getMember("methodName")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getMethodName", intToStringDesc, getMethodName);

        StaticIntrinsic getMethodDesc = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("methodInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle minfoHandle = builder.pointerHandle(builder.bitCast(tablePointer, minfoType.getPointer()), arguments.get(0));
            return builder.load(builder.memberOf(minfoHandle, minfoType.getMember("methodDesc")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getMethodDesc", intToStringDesc, getMethodDesc);

        StaticIntrinsic getTypeId = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("methodInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle minfoHandle = builder.pointerHandle(builder.bitCast(tablePointer, minfoType.getPointer()), arguments.get(0));
            return builder.load(builder.memberOf(minfoHandle, minfoType.getMember("typeId")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getTypeId", intToIntDesc, getTypeId);

        StaticIntrinsic getModifiers = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("methodInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle minfoHandle = builder.pointerHandle(builder.bitCast(tablePointer, minfoType.getPointer()), arguments.get(0));
            return builder.load(builder.memberOf(minfoHandle, minfoType.getMember("modifiers")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getModifiers", intToIntDesc, getModifiers);

        String methodDataClass = "org/qbicc/runtime/stackwalk/MethodData";
        MethodElement getLineNumberElement = methodFinder.getMethod(methodDataClass, "getLineNumber");
        MethodElement getMethodInfoIndexElement = methodFinder.getMethod(methodDataClass, "getMethodInfoIndex");
        MethodElement getFileNameElement = methodFinder.getMethod(methodDataClass, "getFileName");
        MethodElement getClassNameElement = methodFinder.getMethod(methodDataClass, "getClassName");
        MethodElement getMethodNameElement = methodFinder.getMethod(methodDataClass, "getMethodName");
        MethodElement getClassElement = methodFinder.getMethod(methodDataClass, "getClass");

        StaticIntrinsic fillStackTraceElement = (builder, target, arguments) -> {
            DefinedTypeDefinition jls = classContext.findDefinedType("java/lang/StackTraceElement");
            LoadedTypeDefinition jlsVal = jls.load();
            Value scIndex = arguments.get(1);

            Value lineNumber = builder.getFirstBuilder().call(
                builder.staticMethod(getLineNumberElement, getLineNumberElement.getDescriptor(), getLineNumberElement.getType()),
                List.of(scIndex));
            Value minfoIndex = builder.getFirstBuilder().call(
                builder.staticMethod(getMethodInfoIndexElement, getMethodInfoIndexElement.getDescriptor(), getMethodInfoIndexElement.getType()),
                List.of(scIndex));

            Value fileName = builder.getFirstBuilder().call(
                builder.staticMethod(getFileNameElement, getFileNameElement.getDescriptor(), getFileNameElement.getType()),
                List.of(minfoIndex));
            Value classObject = builder.getFirstBuilder().call(
                builder.staticMethod(getClassElement, getClassElement.getDescriptor(), getClassElement.getType()),
                List.of(minfoIndex));
            Value className = builder.getFirstBuilder().call(
                builder.staticMethod(getClassNameElement, getClassNameElement.getDescriptor(), getClassNameElement.getType()),
                List.of(minfoIndex));
            Value methodName = builder.getFirstBuilder().call(
                builder.staticMethod(getMethodNameElement, getMethodNameElement.getDescriptor(), getMethodNameElement.getType()),
                List.of(minfoIndex));

            ValueHandle steRefHandle = builder.referenceHandle(arguments.get(0));
            FieldElement dcField = jlsVal.findField("declaringClass");
            FieldElement mnField = jlsVal.findField("methodName");
            FieldElement fnField = jlsVal.findField("fileName");
            FieldElement lnField = jlsVal.findField("lineNumber");
            FieldElement classField = jlsVal.findField("declaringClassObject");

            builder.store(builder.instanceFieldOf(steRefHandle, dcField), className, MemoryAtomicityMode.NONE);
            builder.store(builder.instanceFieldOf(steRefHandle, mnField), methodName, MemoryAtomicityMode.NONE);
            builder.store(builder.instanceFieldOf(steRefHandle, fnField), fileName, MemoryAtomicityMode.NONE);
            builder.store(builder.instanceFieldOf(steRefHandle, lnField), lineNumber, MemoryAtomicityMode.NONE);
            builder.store(builder.instanceFieldOf(steRefHandle, classField), classObject, MemoryAtomicityMode.NONE);
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType()); // void literal
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "fillStackTraceElement", steIntToVoidDesc, fillStackTraceElement);
    }

    public static void registerJavaLangNumberIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        // Mathematical intrinsics

        ClassTypeDescriptor byteDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Byte");
        ClassTypeDescriptor characterDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Character");
        ClassTypeDescriptor integerDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Integer");
        ClassTypeDescriptor longDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Long");
        ClassTypeDescriptor shortDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Short");

        // binary operations

        MethodDescriptor binaryByteToIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.B, BaseTypeDescriptor.B));
        MethodDescriptor binaryCharToIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.C, BaseTypeDescriptor.C));
        MethodDescriptor binaryIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.I, BaseTypeDescriptor.I));
        MethodDescriptor binaryLongDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.J, BaseTypeDescriptor.J));
        MethodDescriptor binaryShortToIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.S, BaseTypeDescriptor.S));
        MethodDescriptor longIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.J, BaseTypeDescriptor.I));

        StaticIntrinsic divideUnsigned = (builder, target, arguments) ->
            builder.divide(asUnsigned(builder, arguments.get(0)), asUnsigned(builder, arguments.get(1)));

        StaticIntrinsic remainderUnsigned = (builder, target, arguments) ->
            builder.remainder(asUnsigned(builder, arguments.get(0)), asUnsigned(builder, arguments.get(1)));

        intrinsics.registerIntrinsic(integerDesc, "divideUnsigned", binaryIntDesc, divideUnsigned);
        intrinsics.registerIntrinsic(longDesc, "divideUnsigned", binaryLongDesc, divideUnsigned);

        intrinsics.registerIntrinsic(integerDesc, "remainderUnsigned", binaryIntDesc, remainderUnsigned);
        intrinsics.registerIntrinsic(longDesc, "remainderUnsigned", binaryLongDesc, remainderUnsigned);

        /* LLVM backend doesn't understand ror and rol, so avoid generating them
        StaticIntrinsic ror = (builder, target, arguments) ->
            builder.ror(arguments.get(0), arguments.get(1));

        StaticIntrinsic rol = (builder, target, arguments) ->
            builder.rol(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(integerDesc, "rotateRight", binaryIntDesc, ror);
        intrinsics.registerIntrinsic(longDesc, "rotateRight", longIntDesc, ror);

        intrinsics.registerIntrinsic(integerDesc, "rotateLeft", binaryIntDesc, rol);
        intrinsics.registerIntrinsic(longDesc, "rotateLeft", longIntDesc, rol);
        */

        StaticIntrinsic compare = (builder, target, arguments) ->
            builder.cmp(arguments.get(0), arguments.get(1));
        StaticIntrinsic compareUnsigned = (builder, target, arguments) ->
            builder.cmp(asUnsigned(builder, arguments.get(0)), asUnsigned(builder, arguments.get(1)));

        intrinsics.registerIntrinsic(byteDesc, "compare", binaryByteToIntDesc, compare);
        intrinsics.registerIntrinsic(byteDesc, "compareUnsigned", binaryByteToIntDesc, compareUnsigned);
        intrinsics.registerIntrinsic(characterDesc, "compare", binaryCharToIntDesc, compare);
        intrinsics.registerIntrinsic(integerDesc, "compare", binaryIntDesc, compare);
        intrinsics.registerIntrinsic(integerDesc, "compareUnsigned", binaryIntDesc, compareUnsigned);
        intrinsics.registerIntrinsic(shortDesc, "compare", binaryShortToIntDesc, compare);
        intrinsics.registerIntrinsic(shortDesc, "compareUnsigned", binaryShortToIntDesc, compareUnsigned);
    }

    private static void registerJavaLangFloatDoubleMathIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        final var ts = ctxt.getTypeSystem();

        // Mathematical intrinsics

        ClassTypeDescriptor floatDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Float");
        ClassTypeDescriptor doubleDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Double");

        MethodDescriptor floatToIntMethodDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.F));
        MethodDescriptor doubleToLongMethodDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.D));

        StaticIntrinsic floatToRawIntBits = (builder, target, arguments) ->
            builder.bitCast(arguments.get(0), ts.getSignedInteger32Type());
        StaticIntrinsic doubleToRawLongBits = (builder, target, arguments) ->
            builder.bitCast(arguments.get(0), ts.getSignedInteger64Type());

        intrinsics.registerIntrinsic(floatDesc, "floatToRawIntBits", floatToIntMethodDesc, floatToRawIntBits);
        intrinsics.registerIntrinsic(doubleDesc, "doubleToRawLongBits", doubleToLongMethodDesc, doubleToRawLongBits);

        MethodDescriptor intToFloatMethodDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.F, List.of(BaseTypeDescriptor.I));
        MethodDescriptor longToDoubleMethodDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.D, List.of(BaseTypeDescriptor.J));

        StaticIntrinsic intBitsToFloat = (builder, target, arguments) ->
            builder.bitCast(arguments.get(0), ts.getFloat32Type());
        StaticIntrinsic longBitsToDouble = (builder, target, arguments) ->
            builder.bitCast(arguments.get(0), ts.getFloat64Type());

        intrinsics.registerIntrinsic(floatDesc, "intBitsToFloat", intToFloatMethodDesc, intBitsToFloat);
        intrinsics.registerIntrinsic(doubleDesc, "longBitsToDouble", longToDoubleMethodDesc, longBitsToDouble);
    }

    static Value asUnsigned(BasicBlockBuilder builder, Value value) {
        IntegerType type = (IntegerType) value.getType();
        return builder.bitCast(value, type.asUnsigned());
    }

    public static void registerJavaLangObjectIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor jlcDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");

        // Object#getClass()Ljava/lang/Class; --> field read of the "typeId" field
        MethodDescriptor getClassDesc = MethodDescriptor.synthesize(classContext, jlcDesc, List.of());

        InstanceIntrinsic getClassIntrinsic = (builder, instance, target, arguments) -> {
            MethodElement helper = RuntimeMethodFinder.get(ctxt).getMethod("getClassFromObject");
            return builder.getFirstBuilder().call(builder.staticMethod(helper, helper.getDescriptor(), helper.getType()), List.of(instance));
        };

        intrinsics.registerIntrinsic(Phase.ADD, objDesc, "getClass", getClassDesc, getClassIntrinsic);

        // TODO: replace this do nothing stub of notifyAll with real implementation
        MethodDescriptor notifyAllDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());
        InstanceIntrinsic notifyAllIntrinsic = (builder, instance, target, arguments) -> 
            ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType()); // Do nothing
        intrinsics.registerIntrinsic(objDesc, "notifyAll", notifyAllDesc, notifyAllIntrinsic);

        InstanceIntrinsic clone = (builder, instance, target, arguments) -> {
            ValueType instanceType = instance.getType();
            if (instanceType instanceof ReferenceType) {
                ReferenceType referenceType = (ReferenceType) instanceType;
                InterfaceObjectType cloneable = classContext.findDefinedType("java/lang/Cloneable").load().getInterfaceType();
                if (! referenceType.instanceOf(cloneable)) {
                    // synthesize a run time check
                    BlockLabel goAhead = new BlockLabel();
                    BlockLabel throwIt = new BlockLabel();
                    builder.if_(builder.instanceOf(instance, cloneable, 0), goAhead, throwIt);
                    try {
                        builder.begin(throwIt);
                        ClassTypeDescriptor cnseDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/CloneNotSupportedException");
                        Value cnse = builder.getFirstBuilder().new_(cnseDesc);
                        builder.call(builder.getFirstBuilder().constructorOf(cnse, cnseDesc, MethodDescriptor.VOID_METHOD_DESCRIPTOR), List.of());
                        builder.throw_(cnse);
                    } catch (BlockEarlyTermination ignored) {
                        // continue
                    }
                    builder.begin(goAhead);
                }
            }
            return builder.clone(instance);
        };

        intrinsics.registerIntrinsic(objDesc, "clone", MethodDescriptor.synthesize(classContext, objDesc, List.of()), clone);

        // stub - public final native void wait(long timeoutMillis)
        MethodDescriptor waitDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(BaseTypeDescriptor.J));
        InstanceIntrinsic wait = (builder, instance, target, arguments) -> ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getExecutable().getType().getReturnType());
        intrinsics.registerIntrinsic(objDesc, "wait", waitDesc, wait);
    }

    static void registerOrgQbiccCompilerIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        LiteralFactory lf = ctxt.getLiteralFactory();
        CoreClasses coreClasses = CoreClasses.get(ctxt);

        ClassTypeDescriptor ciDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/CompilerIntrinsics");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor clsDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");

        MethodDescriptor newRefArrayDesc =  MethodDescriptor.synthesize(classContext, objDesc, List.of(clsDesc, BaseTypeDescriptor.I, BaseTypeDescriptor.I));
        StaticIntrinsic newRefArray = (builder, target, arguments) -> {
            ReferenceArrayObjectType upperBound = classContext.findDefinedType("java/lang/Object").load().getType().getReferenceArrayObject();
            Value typeId = builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), coreClasses.getClassTypeIdField()));
            Value dims = builder.truncate(arguments.get(1), (WordType) coreClasses.getRefArrayDimensionsField().getType());
            return builder.newReferenceArray(upperBound, typeId, dims, arguments.get(2));
        };
        intrinsics.registerIntrinsic(Phase.ADD, ciDesc, "emitNewReferenceArray", newRefArrayDesc, newRefArray);

        MethodDescriptor newDesc = MethodDescriptor.synthesize(classContext, objDesc, List.of(clsDesc));
        StaticIntrinsic new_ = (builder, target, arguments) -> {
            ClassObjectType upperBound = classContext.findDefinedType("java/lang/Object").load().getClassType();
            Value typeId = builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), coreClasses.getClassTypeIdField()));
            Value instanceSize = builder.extend(builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), coreClasses.getClassInstanceSizeField())), ctxt.getTypeSystem().getSignedInteger64Type());
            Value instanceAlign = builder.extend(builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), coreClasses.getClassInstanceAlignField())), ctxt.getTypeSystem().getSignedInteger32Type());
            return builder.new_(upperBound, typeId, instanceSize, instanceAlign);
        };
        intrinsics.registerIntrinsic(Phase.ADD, ciDesc, "emitNew", newDesc, new_);

        MethodDescriptor copyDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(clsDesc, objDesc, objDesc));
        StaticIntrinsic copy = (builder, target, arguments) -> {
            Value cls = arguments.get(0);
            Value src = arguments.get(1);
            Value dst = arguments.get(2);
            Value size32 = builder.load(builder.instanceFieldOf(builder.referenceHandle(cls), coreClasses.getClassInstanceSizeField()), MemoryAtomicityMode.UNORDERED);
            Value size = builder.extend(size32, ctxt.getTypeSystem().getSignedInteger64Type());

            // TODO: This is a kludge in multiple ways:
            //  1. We should not directly call a NoGc method here.
            //  2. We are overwriting the object header fields initialized by new when doing the copy
            //     (to make sure we copy any instance fields that have been assigned to use the padding bytes in the basic object header).
            MethodElement method = NoGc.get(ctxt).getCopyMethod();
            return builder.call(builder.staticMethod(method, method.getDescriptor(), method.getType()), List.of(dst, src, size));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "copyInstanceFields", copyDesc, copy);
    }

    static void registerOrgQbiccObjectModelIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        CoreClasses coreClasses = CoreClasses.get(ctxt);
        SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
        RuntimeMethodFinder methodFinder = RuntimeMethodFinder.get(ctxt);
        LiteralFactory lf = ctxt.getLiteralFactory();

        ClassTypeDescriptor ciDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/CompilerIntrinsics");
        ClassTypeDescriptor typeIdDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$type_id");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor clsDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor jlsDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor uint8Desc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$uint8_t");
        ClassTypeDescriptor pthreadMutexPtrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/posix/PThread$pthread_mutex_t_ptr");
        ClassTypeDescriptor pthreadPtrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/posix/PThread$pthread_t_ptr");
        ClassTypeDescriptor jltDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Thread");

        MethodDescriptor objTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(objDesc));
        MethodDescriptor objUint8Desc = MethodDescriptor.synthesize(classContext, uint8Desc, List.of(objDesc));
        MethodDescriptor typeIdTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(typeIdDesc));
        MethodDescriptor typeIdBooleanDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(typeIdDesc));
        MethodDescriptor typeIdTypeIdBooleanDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(typeIdDesc, typeIdDesc));
        MethodDescriptor typeIdVoidDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(typeIdDesc));
        MethodDescriptor typeIdIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(typeIdDesc));
        MethodDescriptor typeIdClsDesc = MethodDescriptor.synthesize(classContext, clsDesc, List.of(typeIdDesc, uint8Desc));
        MethodDescriptor typeIdToClassDesc = MethodDescriptor.synthesize(classContext, clsDesc, List.of(typeIdDesc));
        MethodDescriptor clsTypeId = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(clsDesc));
        MethodDescriptor clsUint8 = MethodDescriptor.synthesize(classContext, uint8Desc, List.of(clsDesc));
        MethodDescriptor IntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of());
        MethodDescriptor emptyTotypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of());
        MethodDescriptor typeIdIntToByteDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.B, List.of(typeIdDesc, BaseTypeDescriptor.I));
        MethodDescriptor createClassDesc = MethodDescriptor.synthesize(classContext, clsDesc, List.of(jlsDesc, typeIdDesc, uint8Desc, clsDesc));
        MethodDescriptor clsClsDesc = MethodDescriptor.synthesize(classContext, clsDesc, List.of(clsDesc));
        MethodDescriptor clsClsBooleanDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(clsDesc, clsDesc));
        MethodDescriptor casDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, Collections.nCopies(3, BaseTypeDescriptor.J));

        StaticIntrinsic typeOf = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), coreClasses.getObjectTypeIdField()), MemoryAtomicityMode.UNORDERED);
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "typeIdOf", objTypeIdDesc, typeOf);

        FieldElement elementTypeField = coreClasses.getRefArrayElementTypeIdField();
        StaticIntrinsic elementTypeOf = (builder, target, arguments) -> {
            ValueHandle handle = builder.referenceHandle(builder.bitCast(arguments.get(0), elementTypeField.getEnclosingType().load().getType().getReference()));
            return builder.load(builder.instanceFieldOf(handle, elementTypeField), MemoryAtomicityMode.UNORDERED);
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "elementTypeIdOf", objTypeIdDesc, elementTypeOf);

        FieldElement dimensionsField = coreClasses.getRefArrayDimensionsField();
        StaticIntrinsic dimensionsOf = (builder, target, arguments) -> {
            ValueHandle handle = builder.referenceHandle(builder.bitCast(arguments.get(0), dimensionsField.getEnclosingType().load().getType().getReference()));
            return builder.load(builder.instanceFieldOf(handle, dimensionsField), MemoryAtomicityMode.UNORDERED);
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "dimensionsOf", objUint8Desc, dimensionsOf);

        StaticIntrinsic maxSubClassId = (builder, target, arguments) -> {
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            ValueHandle typeIdStruct = builder.elementOf(builder.globalVariable(typeIdGlobal), arguments.get(0));
            return builder.load(builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("maxSubTypeId")), MemoryAtomicityMode.UNORDERED);
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "maxSubClassTypeIdOf", typeIdTypeIdDesc, maxSubClassId);

        StaticIntrinsic isObject = (builder, target, arguments) -> {
            LoadedTypeDefinition jlo = classContext.findDefinedType("java/lang/Object").load();
            return builder.isEq(arguments.get(0), lf.literalOfType(jlo.getType()));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isJavaLangObject", typeIdBooleanDesc, isObject);

        StaticIntrinsic isCloneable = (builder, target, arguments) -> {
            LoadedTypeDefinition jlc = classContext.findDefinedType("java/lang/Cloneable").load();
            return builder.isEq(arguments.get(0), lf.literalOfType(jlc.getType()));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isJavaLangCloneable", typeIdBooleanDesc, isCloneable);

        StaticIntrinsic isSerializable = (builder, target, arguments) -> {
            LoadedTypeDefinition jis = classContext.findDefinedType("java/io/Serializable").load();
            return builder.isEq(arguments.get(0), lf.literalOfType(jis.getType()));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isJavaIoSerializable", typeIdBooleanDesc, isSerializable);

        StaticIntrinsic isClass = (builder, target, arguments) -> {
            LoadedTypeDefinition jlo = classContext.findDefinedType("java/lang/Object").load();
            ValueType refArray = coreClasses.getArrayLoadedTypeDefinition("[ref").getType();
            Value isObj = builder.isEq(arguments.get(0), lf.literalOfType(jlo.getType()));
            Value isAboveRef = builder.isLt(lf.literalOfType(refArray), arguments.get(0));
            Value isNotInterface = builder.isLt(arguments.get(0), lf.literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), tables.getFirstInterfaceTypeId()));
            return builder.or(isObj, builder.and(isAboveRef, isNotInterface));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isClass", typeIdBooleanDesc, isClass);

        StaticIntrinsic isInterface = (builder, target, arguments) ->
            builder.isLe(lf.literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), tables.getFirstInterfaceTypeId()), arguments.get(0));

        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isInterface", typeIdBooleanDesc, isInterface);

        StaticIntrinsic isPrimArray = (builder, target, arguments) -> {
            ValueType firstPrimArray = coreClasses.getArrayLoadedTypeDefinition("[Z").getType();
            ValueType lastPrimArray = coreClasses.getArrayLoadedTypeDefinition("[D").getType();
            return builder.and(builder.isGe(arguments.get(0), lf.literalOfType(firstPrimArray)),
                builder.isLe(arguments.get(0), lf.literalOfType(lastPrimArray)));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isPrimArray", typeIdBooleanDesc, isPrimArray);

        StaticIntrinsic isPrimitive = (builder, target, arguments) -> {
            ValueType firstPrimType = Primitive.getPrimitiveFor('Z').getType();
            ValueType lastPrimType = Primitive.getPrimitiveFor('V').getType();
            return builder.and(builder.isGe(arguments.get(0), lf.literalOfType(firstPrimType)),
                               builder.isLe(arguments.get(0), lf.literalOfType(lastPrimType)));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isPrimitive", typeIdBooleanDesc, isPrimitive);

        StaticIntrinsic isRefArray = (builder, target, arguments) -> {
            ValueType refArray = coreClasses.getArrayLoadedTypeDefinition("[ref").getType();
            return builder.isEq(arguments.get(0), lf.literalOfType(refArray));
        };

        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isReferenceArray", typeIdBooleanDesc, isRefArray);

        StaticIntrinsic getRefArrayTypeId = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), coreClasses.getRefArrayContentField().getEnclosingType().load().getTypeId());
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getReferenceArrayTypeId", emptyTotypeIdDesc, getRefArrayTypeId);

        StaticIntrinsic doesImplement = (builder, target, arguments) -> {
            IntegerType typeIdLiteralType = ctxt.getTypeSystem().getTypeIdLiteralType();
            Value objTypeId = arguments.get(0);
            Value interfaceTypeId = arguments.get(1);
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            ValueHandle typeIdStruct = builder.elementOf(builder.globalVariable(typeIdGlobal), objTypeId);
            ValueHandle bits = builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("interfaceBits"));
            Value adjustedInterfaceTypeId = builder.sub(interfaceTypeId, lf.literalOf(typeIdLiteralType, tables.getFirstInterfaceTypeId()));
            Value implementsIdx = builder.shr(builder.bitCast(adjustedInterfaceTypeId, typeIdLiteralType.asUnsigned()), lf.literalOf(typeIdLiteralType, 3));
            Value implementsBit = builder.and(adjustedInterfaceTypeId, lf.literalOf(typeIdLiteralType, 7));
            Value dataByte = builder.load(builder.elementOf(bits, builder.extend(implementsIdx, ctxt.getTypeSystem().getSignedInteger32Type())), MemoryAtomicityMode.UNORDERED);
            Value mask = builder.truncate(builder.shl(lf.literalOf(typeIdLiteralType, 1), implementsBit), ctxt.getTypeSystem().getSignedInteger8Type());
            return builder.isEq(mask, builder.and(mask, dataByte));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "doesImplement", typeIdTypeIdBooleanDesc, doesImplement);

        StaticIntrinsic getDimFromClass = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), coreClasses.getClassDimensionField()), MemoryAtomicityMode.UNORDERED);
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getDimensionsFromClass", clsUint8, getDimFromClass);

        StaticIntrinsic getTypeIdFromClass = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), coreClasses.getClassTypeIdField()), MemoryAtomicityMode.UNORDERED);
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getTypeIdFromClass", clsTypeId, getTypeIdFromClass);

        MethodElement getOrCreateArrayClass = methodFinder.getMethod("getOrCreateClassForRefArray");
        StaticIntrinsic getClassFromTypeId = (builder, target, arguments) -> {
            /** Pseudo code for this intrinsic:
             *    Class<?> componentClass = qbicc_jlc_lookup_table[typeId];
             *    Class<?> result = componentClass;
             *    if (dims > 0) {
             *        result = getOrCreateClassForRefArray(componentClass, dims);
             *    }
             *    return result;
             */
            Value typeId = arguments.get(0);
            Value dims = arguments.get(1);
            BlockLabel trueBranch = new BlockLabel();
            BlockLabel fallThrough = new BlockLabel();

            BuildtimeHeap buildtimeHeap = BuildtimeHeap.get(ctxt);
            GlobalVariableElement classArrayGlobal = buildtimeHeap.getAndRegisterGlobalClassArray(builder.getCurrentElement());
            // todo: if this is changed from load to referenceTo, also delete isConstant from ClassOf and fix it in getClassFromTypeIdSimple
            Value componentClass = builder.load(builder.elementOf(builder.globalVariable(classArrayGlobal), typeId), MemoryAtomicityMode.UNORDERED);
            Value result = componentClass;
            PhiValue phi = builder.phi(result.getType(), fallThrough);

            BasicBlock from = builder.if_(builder.isGt(dims, ctxt.getLiteralFactory().literalOf(0)), trueBranch, fallThrough); // if (dimensions > 0)
            phi.setValueForBlock(ctxt, builder.getCurrentElement(), from, result);

            builder.begin(trueBranch); // true; create Class for array reference
            result = builder.getFirstBuilder().call(builder.staticMethod(getOrCreateArrayClass, getOrCreateArrayClass.getDescriptor(), getOrCreateArrayClass.getType()), List.of(componentClass, dims));
            from = builder.goto_(fallThrough);
            phi.setValueForBlock(ctxt, builder.getCurrentElement(), from, result);
            builder.begin(fallThrough);
            return phi;
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getClassFromTypeId", typeIdClsDesc, getClassFromTypeId);

        StaticIntrinsic getClassFromTypeIdSimple = (builder, target, arguments) -> {
            BuildtimeHeap buildtimeHeap = BuildtimeHeap.get(ctxt);
            GlobalVariableElement classArrayGlobal = buildtimeHeap.getAndRegisterGlobalClassArray(builder.getCurrentElement());
            // todo: if this is changed from load to referenceTo, also delete isConstant from ClassOf and fix it in getClassFromTypeId
            return builder.load(builder.elementOf(builder.globalVariable(classArrayGlobal), arguments.get(0)), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getClassFromTypeIdSimple", typeIdToClassDesc, getClassFromTypeIdSimple);

        StaticIntrinsic getArrayClassOf = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), CoreClasses.get(ctxt).getArrayClassField()), MemoryAtomicityMode.UNORDERED);
        intrinsics.registerIntrinsic(ciDesc, "getArrayClassOf", clsClsDesc, getArrayClassOf);

        StaticIntrinsic setArrayClass = (builder, target, arguments) -> {
            LoadedTypeDefinition jlc = classContext.findDefinedType("java/lang/Class").load();
            ReferenceType refType = jlc.getType().getReference();
            Value expect = ctxt.getLiteralFactory().nullLiteralOfType(refType);
            Value update = arguments.get(1);
            Value result = builder.cmpAndSwap(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), CoreClasses.get(ctxt).getArrayClassField()), expect, update, MemoryAtomicityMode.ACQUIRE_RELEASE, MemoryAtomicityMode.ACQUIRE, CmpAndSwap.Strength.STRONG);
            // extract the flag
            return builder.extractMember(result, CmpAndSwap.getResultType(ctxt, refType).getMember(1));
        };
        intrinsics.registerIntrinsic(ciDesc, "setArrayClass", clsClsBooleanDesc, setArrayClass);


        FieldElement jlcName = classContext.findDefinedType("java/lang/Class").load().findField("name");
        FieldElement jlcCompType = classContext.findDefinedType("java/lang/Class").load().findField("componentType");

        StaticIntrinsic createClass = (builder, target, arguments) -> {
            ClassObjectType jlcType = (ClassObjectType) ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load().getType();
            CompoundType compoundType = Layout.get(ctxt).getInstanceLayoutInfo(jlcType.getDefinition()).getCompoundType();
            Value instance = builder.new_(jlcType, lf.literalOfType(jlcType), lf.literalOf(compoundType.getSize()), lf.literalOf(compoundType.getAlign()));
            ValueHandle instanceHandle = builder.referenceHandle(instance);
            ValueHandle handle = builder.instanceFieldOf(instanceHandle, jlcName);
            builder.store(handle, arguments.get(0), handle.getDetectedMode());
            handle = builder.instanceFieldOf(instanceHandle, CoreClasses.get(ctxt).getClassTypeIdField());
            builder.store(handle, arguments.get(1), handle.getDetectedMode());
            handle = builder.instanceFieldOf(builder.referenceHandle(instance), CoreClasses.get(ctxt).getClassDimensionField());
            builder.store(handle, arguments.get(2), handle.getDetectedMode());
            handle = builder.instanceFieldOf(instanceHandle, jlcCompType);
            builder.store(handle, arguments.get(3), handle.getDetectedMode());
            return instance;
        };
        intrinsics.registerIntrinsic(Phase.ADD, ciDesc, "createClass", createClassDesc, createClass);

        StaticIntrinsic getNumberOfTypeIds = (builder, target, arguments) -> lf.literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), tables.get_number_of_typeids());
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getNumberOfTypeIds", emptyTotypeIdDesc, getNumberOfTypeIds);

        StaticIntrinsic callClassInitializer = (builder, target, arguments) -> {
            Value typeId = arguments.get(0);
        
            // CompoundType clinit_state_t =  CompoundType.builder(ts)
            //     .setTag(CompoundType.Tag.STRUCT)
            //     .setName("qbicc_clinit_state")
            //     .setOverallAlignment(ts.getPointerAlignment())
            //     .addNextMember("init_state", init_state_t)
            //     .addNextMember("class_initializers", class_initializers_t)
            //     .build();

            GlobalVariableElement clinitStates = tables.getAndRegisterGlobalClinitStateStruct(builder.getCurrentElement());
            CompoundType clinitStates_t = (CompoundType) clinitStates.getType();
            ValueHandle initializers = builder.memberOf(builder.globalVariable(clinitStates), clinitStates_t.getMember("class_initializers"));
            Value typeIdInit = builder.load(builder.elementOf(initializers, typeId), MemoryAtomicityMode.UNORDERED);

            return builder.call(builder.pointerHandle(typeIdInit), List.of(builder.load(builder.currentThread(), MemoryAtomicityMode.NONE)));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "callClassInitializer", typeIdVoidDesc, callClassInitializer);

        // int get_typeid_flags(CNative.type_id typeID);
        StaticIntrinsic get_typeid_flags = (builder, target, arguments) -> {
            Value typeId = arguments.get(0);
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            ValueHandle typeIdStruct = builder.elementOf(builder.globalVariable(typeIdGlobal), typeId);
            ValueHandle flags = builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("flags"));
            Value flagValue = builder.load(flags, MemoryAtomicityMode.UNORDERED);
            return flagValue;
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getTypeIdFlags", typeIdIntDesc, get_typeid_flags);

        // public static native CNative.type_id getSuperClassTypeId(CNative.type_id typeId);
        StaticIntrinsic getSuperClassTypeId = (builder, target, arguments) -> {
            Value typeId = arguments.get(0);
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            ValueHandle typeIdStruct = builder.elementOf(builder.globalVariable(typeIdGlobal), typeId);
            ValueHandle superTypeId = builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("superTypeId"));
            Value superTypeIdValue = builder.load(superTypeId, MemoryAtomicityMode.UNORDERED);
            return superTypeIdValue;
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getSuperClassTypeId", typeIdTypeIdDesc, getSuperClassTypeId);

        // public static native CNative.type_id getFirstInterfaceTypeId();
        StaticIntrinsic getFirstInterfaceTypeid = (builder, target, arguments) -> {
            return lf.literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), tables.getFirstInterfaceTypeId());
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getFirstInterfaceTypeid", emptyTotypeIdDesc, getFirstInterfaceTypeid);

        // public static native int getNumberOfBytesInInterfaceBitsArray();
        StaticIntrinsic getNumberOfBytesInInterfaceBitsArray = (builder, target, arguments) -> {
            return lf.literalOf(tables.getNumberOfBytesInInterfaceBitsArray());
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getNumberOfBytesInInterfaceBitsArray", IntDesc, getNumberOfBytesInInterfaceBitsArray);

        // public static native byte getByteOfInterfaceBits(CNative.type_id typeId, int index);
        StaticIntrinsic getByteOfInterfaceBits = (builder, target, arguments) -> {
            Value typeId = arguments.get(0);
            Value index = arguments.get(1);
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            ValueHandle typeIdStruct = builder.elementOf(builder.globalVariable(typeIdGlobal), typeId);
            ValueHandle bits = builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("interfaceBits"));
            Value dataByte = builder.load(builder.elementOf(bits, index), MemoryAtomicityMode.UNORDERED);
            return dataByte;
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getByteOfInterfaceBits", typeIdIntToByteDesc, getByteOfInterfaceBits);

        // public static native boolean isInitialized(CNative.type_id typdId);
        StaticIntrinsic isInitialized = (builder, target, arguments) -> {
            Value typeId = arguments.get(0);

            GlobalVariableElement clinitStates = tables.getAndRegisterGlobalClinitStateStruct(builder.getCurrentElement());
            CompoundType clinitStates_t = (CompoundType) clinitStates.getType();
            ValueHandle init_state_array = builder.memberOf(builder.globalVariable(clinitStates), clinitStates_t.getMember("init_state"));
            Value state = builder.load(builder.elementOf(init_state_array, typeId), MemoryAtomicityMode.ACQUIRE);

            return builder.isEq(state, lf.literalOf(1));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isInitialized", typeIdBooleanDesc, isInitialized);

        // public static native void setInitialized(CNative.type_id typdId);
        StaticIntrinsic setInitialized = (builder, target, arguments) -> {
            Value typeId = arguments.get(0);

            GlobalVariableElement clinitStates = tables.getAndRegisterGlobalClinitStateStruct(builder.getCurrentElement());
            CompoundType clinitStates_t = (CompoundType) clinitStates.getType();
            Member init_state_t = clinitStates_t.getMember("init_state");
            IntegerType init_state_element_t = (IntegerType)((ArrayType)clinitStates_t.getMember("init_state").getType()).getElementType();
            ValueHandle init_state_array = builder.memberOf(builder.globalVariable(clinitStates), init_state_t);
            builder.store(builder.elementOf(init_state_array, typeId), lf.literalOf(init_state_element_t, 1), MemoryAtomicityMode.RELEASE);

            return lf.zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());

        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "setInitialized", typeIdVoidDesc, setInitialized);

        FieldElement nativeObjectMonitorField = CoreClasses.get(ctxt).getObjectNativeObjectMonitorField();
        // PThread.pthread_mutex_t_ptr getNativeObjectMonitor(Object reference);
        MethodDescriptor nomOfDesc = MethodDescriptor.synthesize(classContext, pthreadMutexPtrDesc, List.of(objDesc));
        StaticIntrinsic nomOf = (builder, target, arguments) -> {
            Value mutexSlot = builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), nativeObjectMonitorField), MemoryAtomicityMode.NONE);
            PointerType returnType = (PointerType)target.getExecutable().getType().getReturnType();
            return builder.valueConvert(mutexSlot, returnType);
        };
        intrinsics.registerIntrinsic(ciDesc, "getNativeObjectMonitor", nomOfDesc, nomOf);

        // boolean setNativeObjectMonitor(Object object, PThread.pthread_mutex_t_ptr nom);
        MethodDescriptor setNomDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(objDesc, pthreadMutexPtrDesc));
        StaticIntrinsic setNom = (builder, target, arguments) -> {
            ValueHandle casTarget = builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), nativeObjectMonitorField);
            Value expect = ctxt.getLiteralFactory().literalOf(0L);
            Value update = builder.valueConvert(arguments.get(1), (SignedIntegerType)nativeObjectMonitorField.getType());
            Value result = builder.cmpAndSwap(casTarget, expect, update, MemoryAtomicityMode.ACQUIRE_RELEASE, MemoryAtomicityMode.ACQUIRE, CmpAndSwap.Strength.STRONG);
            return builder.extractMember(result, CmpAndSwap.getResultType(ctxt, update.getType()).getMember(1));
        };
        intrinsics.registerIntrinsic(ciDesc, "setNativeObjectMonitor", setNomDesc, setNom);
    }

    static void registerOrgQbiccRuntimeMainIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor mainDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/Main");

        ClassTypeDescriptor tgDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ThreadGroup");
        MethodDescriptor voidVoidDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());

        // Construct system thread group
        StaticIntrinsic sysThrGrp = (builder, target, arguments) -> {
            Value tg = builder.new_(tgDesc);
            builder.call(builder.constructorOf(tg, tgDesc, voidVoidDesc), List.of());
            return tg;
        };

        MethodDescriptor returnTgDesc = MethodDescriptor.synthesize(classContext, tgDesc, List.of());

        intrinsics.registerIntrinsic(mainDesc, "createSystemThreadGroup", returnTgDesc, sysThrGrp);

    }

    static ValueHandle getTarget(CompilationContext ctxt, BasicBlockBuilder builder, Value input) {
        if (input instanceof Load) {
            Load load = (Load) input;
            ValueHandle target = load.getValueHandle();
            // make sure the target is unambiguous
            if (target instanceof Variable) {
                ValueType valueType = target.getValueType();
                if (valueType instanceof PointerType) {
                    ctxt.error(builder.getLocation(), "Ambiguous target for operation; to target the pointer value, use loadUnshared(val); to target the variable use addr_of(val)");
                }
            }
            return target;
        } else {
            ctxt.error(builder.getLocation(), "Cannot determine target of operation");
            return null;
        }
    }

    public static void registerJavaLangMathIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor mathDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Math");
        ClassTypeDescriptor strictDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/StrictMath");

        MethodDescriptor intIntIntDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, Collections.nCopies(2, BaseTypeDescriptor.I));
        MethodDescriptor longLongLongDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, Collections.nCopies(2, BaseTypeDescriptor.J));
        MethodDescriptor floatFloatFloatDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.F, Collections.nCopies(2, BaseTypeDescriptor.F));
        MethodDescriptor doubleDoubleDoubleDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.D, Collections.nCopies(2, BaseTypeDescriptor.D));

        StaticIntrinsic min = (builder, target, arguments) ->
            builder.min(arguments.get(0), arguments.get(1));

        StaticIntrinsic max = (builder, target, arguments) ->
            builder.max(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(mathDesc, "min", intIntIntDescriptor, min);
        intrinsics.registerIntrinsic(mathDesc, "min", longLongLongDescriptor, min);
        intrinsics.registerIntrinsic(mathDesc, "min", floatFloatFloatDescriptor, min);
        intrinsics.registerIntrinsic(mathDesc, "min", doubleDoubleDoubleDescriptor, min);

        intrinsics.registerIntrinsic(mathDesc, "max", intIntIntDescriptor, max);
        intrinsics.registerIntrinsic(mathDesc, "max", longLongLongDescriptor, max);
        intrinsics.registerIntrinsic(mathDesc, "max", floatFloatFloatDescriptor, max);
        intrinsics.registerIntrinsic(mathDesc, "max", doubleDoubleDoubleDescriptor, max);

        intrinsics.registerIntrinsic(strictDesc, "min", intIntIntDescriptor, min);
        intrinsics.registerIntrinsic(strictDesc, "min", longLongLongDescriptor, min);
        intrinsics.registerIntrinsic(strictDesc, "min", floatFloatFloatDescriptor, min);
        intrinsics.registerIntrinsic(strictDesc, "min", doubleDoubleDoubleDescriptor, min);

        intrinsics.registerIntrinsic(strictDesc, "max", intIntIntDescriptor, max);
        intrinsics.registerIntrinsic(strictDesc, "max", longLongLongDescriptor, max);
        intrinsics.registerIntrinsic(strictDesc, "max", floatFloatFloatDescriptor, max);
        intrinsics.registerIntrinsic(strictDesc, "max", doubleDoubleDoubleDescriptor, max);
    }

    /* Temporary workaround for casting in VMHelpers */
    static void registerOrgQbiccRuntimePosixPthreadCastPtr(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor cnativeDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative");
        ClassTypeDescriptor ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$ptr");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");

        /* intrinsic implementation */
        StaticIntrinsic castPtr = (builder, target, arguments) -> {
            Value castObject = arguments.get(0);
            Value typeValue = arguments.get(1);
            if (typeValue instanceof ClassOf) {
                ClassOf typeClassOf = (ClassOf)typeValue;
                Value typeInput = typeClassOf.getInput();
                if (typeInput instanceof TypeLiteral) {
                    ValueType type = ((TypeLiteral) typeInput).getValue();
                    PointerType newPointerType = type.getPointer();
                    return builder.bitCast(castObject, newPointerType);
                }
            }
            ctxt.error(builder.getLocation(), "Invalid pointer type.");
            return castObject;
        };

        intrinsics.registerIntrinsic(cnativeDesc, "castPtr", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(ptrDesc, classDesc)), castPtr);
    }

    private static void registerJavaLangRefIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor referenceDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ref/Reference");
        ClassTypeDescriptor phantomReferenceDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ref/PhantomReference");

        MethodDescriptor objToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(objDesc));

        InstanceIntrinsic refersTo0 = (builder, instance, target, arguments) ->
            builder.isEq(
                arguments.get(0),
                builder.load(builder.instanceFieldOf(builder.referenceHandle(instance), referenceDesc, "referent", objDesc), MemoryAtomicityMode.UNORDERED)
            );

        intrinsics.registerIntrinsic(Phase.ADD, referenceDesc, "refersTo0", objToBool, refersTo0);
        intrinsics.registerIntrinsic(Phase.ADD, phantomReferenceDesc, "refersTo0", objToBool, refersTo0);
    }

    private static void registerJavaLangRuntimeIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor runtimeClassDescriptor = ClassTypeDescriptor.synthesize(classContext, "java/lang/Runtime");

        InstanceIntrinsic availableProcessorsIntrinsic = (builder, instance, target, arguments) -> {
            // TODO this should reflect the target platform
            int numProcessors = Runtime.getRuntime().availableProcessors();
            return ctxt.getLiteralFactory().literalOf(numProcessors);
        };

        MethodDescriptor availableProcessorsMethodDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of());

        intrinsics.registerIntrinsic(runtimeClassDescriptor, "availableProcessors", availableProcessorsMethodDesc, availableProcessorsIntrinsic);
    }

    private static Value traverseLoads(Value value) {
        // todo: modify Load to carry a "known value"?
        if (value instanceof Load) {
            ValueHandle valueHandle = value.getValueHandle();
            if (valueHandle instanceof LocalVariable || valueHandle instanceof Variable && ((Variable) valueHandle).getVariableElement().isFinal()) {
                Node dependency = value;
                while (dependency instanceof OrderedNode) {
                    dependency = ((OrderedNode) dependency).getDependency();
                    if (dependency instanceof Store) {
                        if (dependency.getValueHandle().equals(valueHandle)) {
                            return ((Store) dependency).getValue();
                        }
                    }
                    if (dependency instanceof BlockEntry) {
                        // not resolvable
                        break;
                    }
                }
            }
        }
        return value;
    }

    private static void registerJavaUtilConcurrentAtomicLongIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor atomicLongDesc = ClassTypeDescriptor.synthesize(classContext, "java/util/concurrent/atomic/AtomicLong");

        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        StaticIntrinsic VMSupportsCS8 = (builder, target, arguments) -> ctxt.getLiteralFactory().literalOf(true);

        intrinsics.registerIntrinsic(atomicLongDesc, "VMSupportsCS8", emptyToBool, VMSupportsCS8);
    }

    private static void registerJDKInternalIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor signalDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/Signal");
        ClassTypeDescriptor jls = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor jlo = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor classloader = ClassTypeDescriptor.synthesize(classContext, "java/lang/ClassLoader");
        ClassTypeDescriptor unixDispatcher = ClassTypeDescriptor.synthesize(classContext, "sun/nio/fs/UnixNativeDispatcher");

        MethodDescriptor stringToInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(jls));
        MethodDescriptor intLongToLong = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.I, BaseTypeDescriptor.J));
        MethodDescriptor boolStringObj = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(jls, jlo));
        MethodDescriptor emptyToByteArray = MethodDescriptor.synthesize(classContext, ArrayTypeDescriptor.of(classContext, BaseTypeDescriptor.B), List.of());
        MethodDescriptor emptyToInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of());

        StaticIntrinsic findSignal = (builder, target, arguments) -> ctxt.getLiteralFactory().literalOf(-1); // TODO: real implementation
        StaticIntrinsic handle = (builder, target, arguments) -> ctxt.getLiteralFactory().literalOf(0L); // TODO: real implementation

        intrinsics.registerIntrinsic(Phase.LOWER, signalDesc, "findSignal0", stringToInt, findSignal);
        intrinsics.registerIntrinsic(signalDesc, "handle0", intLongToLong, handle);

        StaticIntrinsic getcwd = (builder, target, arguments) -> ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getSignedInteger8Type().getPrimitiveArrayObjectType().getReference()); // TODO: real implementation
        StaticIntrinsic init = (builder, target, arguments) -> ctxt.getLiteralFactory().literalOf(0); // TODO: real implementation

        intrinsics.registerIntrinsic(Phase.LOWER, unixDispatcher, "getcwd", emptyToByteArray, getcwd);
        intrinsics.registerIntrinsic(unixDispatcher, "init", emptyToInt, init);

        // ClassLoader.trySetObjectField; to avoid problem with non-literal string to objectFieldOffset in a helper method
        InstanceIntrinsic trySetObjectField = (builder, input, target, arguments) -> {
            Value string = arguments.get(0);
            Value newValue = arguments.get(1);
            LiteralFactory lf = ctxt.getLiteralFactory();

            String fieldName = null;
            if (string instanceof StringLiteral) {
                fieldName = ((StringLiteral) string).getValue();
            } else if (string instanceof ObjectLiteral) {
                VmObject vmObject = ((ObjectLiteral) string).getValue();
                if (vmObject instanceof VmString) {
                    fieldName = ((VmString) vmObject).getContent();
                }
            }
            if (fieldName == null) {
                ctxt.error(builder.getLocation(), "trySetObjectField string argument must be a literal string");
                return lf.literalOf(false);
            }
            LoadedTypeDefinition ltd = ctxt.getBootstrapClassContext().findDefinedType("java/lang/ClassLoader").load();
            FieldElement field = ltd.findField(fieldName);
            if (field == null) {
                ctxt.error(builder.getLocation(), "No such field \"%s\" on class \"%s\"", fieldName, ltd.getVmClass().getName());
                return lf.literalOf(false);
            }

            ValueType expectType = newValue.getType();
            Value result = builder.cmpAndSwap(builder.instanceFieldOf(builder.referenceHandle(input), field), lf.zeroInitializerLiteralOfType(expectType),
                newValue, MemoryAtomicityMode.VOLATILE, MemoryAtomicityMode.MONOTONIC, CmpAndSwap.Strength.STRONG);
            // result is a compound structure; extract the success flag
            return builder.extractMember(result, CmpAndSwap.getResultType(ctxt, expectType).getMember(1));
        };



        intrinsics.registerIntrinsic(classloader, "trySetObjectField", boolStringObj, trySetObjectField);
    }
}

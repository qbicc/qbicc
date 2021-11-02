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
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.Extend;
import org.qbicc.graph.GlobalVariable;
import org.qbicc.graph.Load;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Store;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.Variable;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.machine.probe.CProbe;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayTables;
import org.qbicc.plugin.intrinsics.InstanceIntrinsic;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.plugin.intrinsics.StaticIntrinsic;
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
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeType;
import org.qbicc.type.UnsignedIntegerType;
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
        registerOrgQbiccRuntimeCNativeIntrinsics(ctxt);
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
        registerJavaLangReflectIntrinsics(ctxt);
        registerJavaLangRuntimeIntrinsics(ctxt);
        registerOrgQbiccObjectModelIntrinsics(ctxt);
        registerOrgQbiccRuntimeMainIntrinsics(ctxt);
        registerOrgQbiccRuntimeValuesIntrinsics(ctxt);
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

        InstanceIntrinsic cast = (builder, instance, target, arguments) -> {
            // TODO: Once we support java.lang.Class literals, we should add a check here to
            //  emit a CheckCast node instead of a call to the helper method if `instance` is a Class literal.
            MethodElement helper = ctxt.getVMHelperMethod("checkcast_class");
            builder.getFirstBuilder().call(builder.staticMethod(helper, helper.getDescriptor(), helper.getType()), List.of(arguments.get(0), instance));

            // Generics erasure issue. The return type of Class<T>.cast is T, but it gets wiped to Object.
            // If the result of this cast is actually used as a T, there will be a (redundant) checkcast bytecode following this operation.
            ReferenceType jlot = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load().getType().getReference();
            return builder.bitCast(arguments.get(0), jlot);
        };

        InstanceIntrinsic initClassName = (builder, instance, target, arguments) -> {
            // not reachable; we always would initialize our class name eagerly
            throw new BlockEarlyTermination(builder.unreachable());
        };

        InstanceIntrinsic isArray = (builder, instance, target, arguments) -> {
            Value id = builder.load(builder.instanceFieldOf(builder.referenceHandle(instance),  coreClasses.getClassTypeIdField()), MemoryAtomicityMode.UNORDERED);
            MethodElement isRefArray = ctxt.getOMHelperMethod("is_reference_array");
            MethodElement isPrimArray = ctxt.getOMHelperMethod("is_prim_array");
            return builder.or(builder.getFirstBuilder().call(builder.staticMethod(isRefArray, isRefArray.getDescriptor(), isRefArray.getType()), List.of(id)),
                              builder.getFirstBuilder().call(builder.staticMethod(isPrimArray, isPrimArray.getDescriptor(), isPrimArray.getType()), List.of(id)));
        };

        InstanceIntrinsic isInterface = (builder, instance, target, arguments) -> {
            Value id = builder.load(builder.instanceFieldOf(builder.referenceHandle(instance),  coreClasses.getClassTypeIdField()), MemoryAtomicityMode.UNORDERED);
            MethodElement isIntf = ctxt.getOMHelperMethod("is_interface");
            return builder.getFirstBuilder().call(builder.staticMethod(isIntf, isIntf.getDescriptor(), isIntf.getType()), List.of(id));
        };

        InstanceIntrinsic isPrimitive = (builder, instance, target, arguments) -> {
            Value id = builder.load(builder.instanceFieldOf(builder.referenceHandle(instance), coreClasses.getClassTypeIdField()), MemoryAtomicityMode.UNORDERED);
            MethodElement isPrim = ctxt.getOMHelperMethod("is_primitive");
            return builder.getFirstBuilder().call(builder.staticMethod(isPrim, isPrim.getDescriptor(), isPrim.getType()), List.of(id));
        };

        InstanceIntrinsic getSuperclass = (builder, instance, target, arguments) -> {
            Value id = builder.load(builder.instanceFieldOf(builder.referenceHandle(instance), coreClasses.getClassTypeIdField()), MemoryAtomicityMode.UNORDERED);
            LiteralFactory lf = ctxt.getLiteralFactory();
            TypeSystem ts = ctxt.getTypeSystem();
            MethodElement helper = ctxt.getVMHelperMethod("get_superclass");
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

        intrinsics.registerIntrinsic(jlcDesc, "cast", objToObj, cast);
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
            MethodElement vmhForName = ctxt.getVMHelperMethod("classForName");
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
            Value ex = builder.new_(uoeType);
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
        ClassTypeDescriptor vmHelpersDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/VMHelpers");
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
        StaticIntrinsic currentThread = (builder, target, arguments) -> builder.currentThread();
        intrinsics.registerIntrinsic(jltDesc, "currentThread", returnJlt, currentThread);

        /* VMHelpers.java - threadWrapper: helper method for java.lang.Thread.start0 */
        MethodDescriptor threadWrapperNativeDesc = MethodDescriptor.synthesize(classContext, voidPtrDesc, List.of(voidPtrDesc));
        StaticIntrinsic threadWrapperNative = (builder, target, arguments) -> {
            Value threadVoidPtr = arguments.get(0);

            DefinedTypeDefinition jlt = classContext.findDefinedType("java/lang/Thread");
            LoadedTypeDefinition jltVal = jlt.load();
            ValueType jltType = jltVal.getType().getReference();
            Value threadObject = builder.bitCast(threadVoidPtr, (WordType)jltType);
            ValueHandle threadObjectHandle = builder.referenceHandle(threadObject);

            /* set current thread */
            ValueHandle qbiccCurrentThreadPointer = builder.pointerHandle(ctxt.getCurrentThreadLocalSymbolLiteral());
            builder.store(qbiccCurrentThreadPointer, threadObject, MemoryAtomicityMode.NONE);

            /* call "run" method of thread object */
            VirtualMethodElementHandle runHandle = (VirtualMethodElementHandle)builder.virtualMethodOf(threadObject, jltDesc, "run", voidDesc);
            builder.call(runHandle, List.of());

            /* set java.lang.Thread.threadStatus to terminated */
            ValueHandle threadStatusHandle = builder.instanceFieldOf(threadObjectHandle, jltDesc, "threadStatus", BaseTypeDescriptor.I);
            builder.store(threadStatusHandle, ctxt.getLiteralFactory().literalOf(threadTerminated), MemoryAtomicityMode.NONE);

            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getExecutable().getType().getReturnType()); /* return null */
        };
        intrinsics.registerIntrinsic(vmHelpersDesc, "threadWrapperNative", threadWrapperNativeDesc, threadWrapperNative);

        /* VMHelpers.java - saveNativeThread: helper method for java.lang.Thread.start0 */
        MethodDescriptor saveNativeThreadDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(voidPtrDesc, pthreadPtrDesc));
        StaticIntrinsic saveNativeThread = (builder, target, arguments) -> {
            // TODO implement
            return ctxt.getLiteralFactory().literalOf(true);
        };
        intrinsics.registerIntrinsic(vmHelpersDesc, "saveNativeThread", saveNativeThreadDesc, saveNativeThread);

        /* private native void start0(); */
        InstanceIntrinsic start0 = (builder, instance, target, arguments) -> {
            ValueType voidPointerType = ctxt.getTypeSystem().getVoidType().getPointer();

            /* set java.lang.Thread.threadStatus to runnable and alive */
            ValueHandle threadStatusHandle = builder.instanceFieldOf(builder.referenceHandle(instance), jltDesc, "threadStatus", BaseTypeDescriptor.I);
            builder.store(threadStatusHandle, ctxt.getLiteralFactory().literalOf(threadRunnable | threadAlive), MemoryAtomicityMode.NONE);

            /* pass threadWrapper as function_ptr - TODO this will eventually be replaced by a call to CNative.addr_of_function */
            MethodDescriptor threadWrapperDesc = MethodDescriptor.synthesize(classContext, voidPtrDesc, List.of(voidPtrDesc));
            ValueHandle threadWrapperValueHandle = builder.staticMethod(vmHelpersDesc, "threadWrapper", threadWrapperDesc);
            Value threadWrapperFunctionPointer = builder.addressOf(threadWrapperValueHandle);

            /* call threadWrapper with null parameter so it does nothing - TODO this is a workaround to create a declares statement for threadWrapper in java.lang.Thread */
            builder.call(threadWrapperValueHandle, List.of(ctxt.getLiteralFactory().zeroInitializerLiteralOfType(voidPointerType)));

            /* pass java.lang.Thread object as ptr<void> */
            Value threadVoidPtr = builder.bitCast(instance, (WordType)voidPointerType);

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
                List.of());
            ClassObjectType jsfcClassType = (ClassObjectType) ctxt.getBootstrapClassContext().findDefinedType(jsfcClass).load().getType();
            Value visitor = builder.getFirstBuilder().new_(jsfcClassType);
            builder.call(
                builder.getFirstBuilder().constructorOf(visitor, jsfcConstructor, jsfcConstructor.getDescriptor(), jsfcConstructor.getType()),
                List.of(frameCount));
            builder.call(
                builder.getFirstBuilder().staticMethod(walkStackElement, walkStackElement.getDescriptor(), walkStackElement.getType()),
                List.of(visitor));

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
            return builder.load(builder.elementOf(builder.pointerHandle(tablePointer), arguments.get(0)), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getInstructionAddress", intToLongDesc, getInstructionAddress);

        StaticIntrinsic getSourceCodeInfoIndex = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("sourceCodeIndexTable")), MemoryAtomicityMode.UNORDERED);
            return builder.load(builder.elementOf(builder.pointerHandle(tablePointer), arguments.get(0)), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getSourceCodeInfoIndex", intToIntDesc, getSourceCodeInfoIndex);

        StaticIntrinsic getMethodInfoIndex = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("sourceCodeInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle scInfoHandle = builder.elementOf(builder.pointerHandle(builder.bitCast(tablePointer, scInfoType.getPointer())), arguments.get(0));
            return builder.load(builder.memberOf(scInfoHandle, scInfoType.getMember("methodInfoIndex")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getMethodInfoIndex", intToIntDesc, getMethodInfoIndex);

        StaticIntrinsic getLineNumber = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("sourceCodeInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle scInfoHandle = builder.elementOf(builder.pointerHandle(builder.bitCast(tablePointer, scInfoType.getPointer())), arguments.get(0));
            return builder.load(builder.memberOf(scInfoHandle, scInfoType.getMember("lineNumber")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getLineNumber", intToIntDesc, getLineNumber);

        StaticIntrinsic getBytecodeIndex = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("sourceCodeInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle scInfoHandle = builder.elementOf(builder.pointerHandle(builder.bitCast(tablePointer, scInfoType.getPointer())), arguments.get(0));
            return builder.load(builder.memberOf(scInfoHandle, scInfoType.getMember("bcIndex")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getBytecodeIndex", intToIntDesc, getBytecodeIndex);

        StaticIntrinsic getInlinedAtIndex = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("sourceCodeInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle scInfoHandle = builder.elementOf(builder.pointerHandle(builder.bitCast(tablePointer, scInfoType.getPointer())), arguments.get(0));
            return builder.load(builder.memberOf(scInfoHandle, scInfoType.getMember("inlinedAtIndex")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getInlinedAtIndex", intToIntDesc, getInlinedAtIndex);

        StaticIntrinsic getFileName = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("methodInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle minfoHandle = builder.elementOf(builder.pointerHandle(builder.bitCast(tablePointer, minfoType.getPointer())), arguments.get(0));
            return builder.load(builder.memberOf(minfoHandle, minfoType.getMember("fileName")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getFileName", intToStringDesc, getFileName);

        StaticIntrinsic getClassName = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("methodInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle minfoHandle = builder.elementOf(builder.pointerHandle(builder.bitCast(tablePointer, minfoType.getPointer())), arguments.get(0));
            return builder.load(builder.memberOf(minfoHandle, minfoType.getMember("className")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getClassName", intToStringDesc, getClassName);

        StaticIntrinsic getMethodName = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("methodInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle minfoHandle = builder.elementOf(builder.pointerHandle(builder.bitCast(tablePointer, minfoType.getPointer())), arguments.get(0));
            return builder.load(builder.memberOf(minfoHandle, minfoType.getMember("methodName")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getMethodName", intToStringDesc, getMethodName);

        StaticIntrinsic getMethodDesc = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("methodInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle minfoHandle = builder.elementOf(builder.pointerHandle(builder.bitCast(tablePointer, minfoType.getPointer())), arguments.get(0));
            return builder.load(builder.memberOf(minfoHandle, minfoType.getMember("methodDesc")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getMethodDesc", intToStringDesc, getMethodDesc);

        StaticIntrinsic getTypeId = (builder, target, arguments) -> {
            GlobalVariable gmdVariable = (GlobalVariable) builder.globalVariable(mdTypes.getAndRegisterGlobalMethodData(builder.getCurrentElement()));
            Value tablePointer = builder.load(builder.memberOf(gmdVariable, gmdType.getMember("methodInfoTable")), MemoryAtomicityMode.UNORDERED);

            ValueHandle minfoHandle = builder.elementOf(builder.pointerHandle(builder.bitCast(tablePointer, minfoType.getPointer())), arguments.get(0));
            return builder.load(builder.memberOf(minfoHandle, minfoType.getMember("typeId")), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, mdDesc, "getTypeId", intToIntDesc, getTypeId);

        String methodDataClass = "org/qbicc/runtime/stackwalk/MethodData";
        MethodElement getLineNumberElement = methodFinder.getMethod(methodDataClass, "getLineNumber");
        MethodElement getMethodInfoIndexElement = methodFinder.getMethod(methodDataClass, "getMethodInfoIndex");
        MethodElement getFileNameElement = methodFinder.getMethod(methodDataClass, "getFileName");
        MethodElement getClassNameElement = methodFinder.getMethod(methodDataClass, "getClassName");
        MethodElement getMethodNameElement = methodFinder.getMethod(methodDataClass, "getMethodName");
        MethodElement getTypeIdElement = methodFinder.getMethod(methodDataClass, "getTypeId");
        MethodElement getClassFromTypeIdElement = methodFinder.getMethod("org/qbicc/runtime/main/ObjectModel", "get_class_from_type_id");

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
            Value className = builder.getFirstBuilder().call(
                builder.staticMethod(getClassNameElement, getClassNameElement.getDescriptor(), getClassNameElement.getType()),
                List.of(minfoIndex));
            Value methodName = builder.getFirstBuilder().call(
                builder.staticMethod(getMethodNameElement, getMethodNameElement.getDescriptor(), getMethodNameElement.getType()),
                List.of(minfoIndex));
            Value typeId = builder.getFirstBuilder().call(
                builder.staticMethod(getTypeIdElement, getTypeIdElement.getDescriptor(), getTypeIdElement.getType()),
                List.of(minfoIndex));
            Value classObject = builder.getFirstBuilder().call(
                builder.staticMethod(getClassFromTypeIdElement, getClassFromTypeIdElement.getDescriptor(), getClassFromTypeIdElement.getType()),
                List.of(typeId, ctxt.getLiteralFactory().literalOf(0)));

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
            MethodElement helper = ctxt.getVMHelperMethod("get_class");
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

    static Literal literalOf(CompilationContext ctxt, boolean v) {
        return ctxt.getLiteralFactory().literalOf(v);
    }

    static void registerOrgQbiccRuntimeCNativeIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor cNativeDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative");
        ClassTypeDescriptor typeIdDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$type_id");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ArrayTypeDescriptor objArrayDesc = ArrayTypeDescriptor.of(classContext, objDesc);
        ClassTypeDescriptor nObjDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$object");
        ClassTypeDescriptor ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$ptr");
        ClassTypeDescriptor wordDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$word");
        ClassTypeDescriptor tgDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ThreadGroup");
        ClassTypeDescriptor thrDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Thread");
        ClassTypeDescriptor strDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");

        ClassTypeDescriptor boolPtrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$_Bool_ptr");

        ClassTypeDescriptor float32ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$_Float32_ptr");
        ClassTypeDescriptor float64ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$_Float64_ptr");

        ClassTypeDescriptor uint16ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$uint16_t_ptr");

        ClassTypeDescriptor int8ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$int8_t_ptr");
        ClassTypeDescriptor int16ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$int16_t_ptr");
        ClassTypeDescriptor int32ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$int32_t_ptr");
        ClassTypeDescriptor int64ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$int64_t_ptr");

        ClassTypeDescriptor ptrDiffTDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stddef$ptrdiff_t");
        ClassTypeDescriptor sizeTDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stddef$size_t");

        MethodDescriptor objTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(objDesc));
        MethodDescriptor objArrayTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(objArrayDesc));

        StaticIntrinsic typeOf = (builder, target, arguments) ->
            builder.typeIdOf(builder.referenceHandle(arguments.get(0)));

        intrinsics.registerIntrinsic(cNativeDesc, "type_id_of", objTypeIdDesc, typeOf);

        FieldElement elementTypeField = CoreClasses.get(ctxt).getRefArrayElementTypeIdField();

        StaticIntrinsic elementTypeOf = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), elementTypeField), MemoryAtomicityMode.UNORDERED);

        intrinsics.registerIntrinsic(cNativeDesc, "element_type_id_of", objArrayTypeIdDesc, elementTypeOf);

        StaticIntrinsic addrOf = (builder, target, arguments) -> {
            Value value = arguments.get(0);
            if (value instanceof BitCast) {
                value = ((BitCast)value).getInput();
            }
            if (value instanceof Extend) {
                value = ((Extend) value).getInput();
            }
            if (value instanceof Load) {
                Load load = (Load) value;
                return builder.addressOf(load.getValueHandle());
            } else {
                ctxt.error(builder.getLocation(), "Cannot take address of value");
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(value.getType().getPointer());
            }
        };

        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, int8ptrDesc, List.of(BaseTypeDescriptor.B)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, uint16ptrDesc, List.of(BaseTypeDescriptor.C)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, float64ptrDesc, List.of(BaseTypeDescriptor.D)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, float32ptrDesc, List.of(BaseTypeDescriptor.F)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, int32ptrDesc, List.of(BaseTypeDescriptor.I)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, int64ptrDesc, List.of(BaseTypeDescriptor.J)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, int16ptrDesc, List.of(BaseTypeDescriptor.S)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, boolPtrDesc, List.of(BaseTypeDescriptor.Z)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(nObjDesc)), addrOf);

        StaticIntrinsic attachNewThread = (builder, target, arguments) -> {
            //java.lang.Thread.nextThreadID
            Value thread = builder.new_(thrDesc);
            // immediately set the thread to be the current thread
            builder.store(builder.pointerHandle(ctxt.getCurrentThreadLocalSymbolLiteral()), thread, MemoryAtomicityMode.NONE);
            // now start initializing
            DefinedTypeDefinition jlt = classContext.findDefinedType("java/lang/Thread");
            LoadedTypeDefinition jltVal = jlt.load();
            // find all the fields
            FieldElement nameFld = jltVal.findField("name");
            FieldElement tidFld = jltVal.findField("tid");
            FieldElement groupFld = jltVal.findField("group");
            FieldElement threadStatusFld = jltVal.findField("threadStatus");
            FieldElement priorityFld = jltVal.findField("priority");

            ValueHandle threadRef = builder.referenceHandle(thread);
            builder.store(builder.instanceFieldOf(threadRef, nameFld), arguments.get(0), MemoryAtomicityMode.NONE);
            builder.store(builder.instanceFieldOf(threadRef, groupFld), arguments.get(1), MemoryAtomicityMode.NONE);
            Value tid = builder.call(builder.staticMethod(thrDesc, "nextThreadID", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of())), List.of());
            builder.store(builder.instanceFieldOf(threadRef, tidFld), tid, MemoryAtomicityMode.NONE);
            // priority default is Thread.NORM_PRIORITY
            Value normPriority = builder.load(builder.staticField(jltVal.findField("NORM_PRIORITY")), MemoryAtomicityMode.NONE);
            builder.store(builder.instanceFieldOf(threadRef, priorityFld), normPriority, MemoryAtomicityMode.NONE);

            // set thread to be running with JVMTI status for RUNNABLE and ALIVE
            builder.store(builder.instanceFieldOf(threadRef, threadStatusFld), ctxt.getLiteralFactory().literalOf(0x05), MemoryAtomicityMode.NONE);
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());
        };

        intrinsics.registerIntrinsic(cNativeDesc, "attachNewThread", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(
            strDesc, tgDesc
        )), attachNewThread);

        InstanceIntrinsic xxxValue = (builder, instance, target, arguments) -> {
            WordType to = (WordType) target.getExecutable().getType().getReturnType();
            return smartConvert(builder, instance, to, true);
        };

        intrinsics.registerIntrinsic(wordDesc, "byteValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.B, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "booleanValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "charValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.C, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "doubleValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.D, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "floatValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.F, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "intValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "longValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "shortValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.S, List.of()), xxxValue);

        InstanceIntrinsic isZero = (builder, instance, target, arguments) -> builder.isEq(instance, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType()));

        intrinsics.registerIntrinsic(wordDesc, "isZero", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of()), isZero);
        intrinsics.registerIntrinsic(wordDesc, "isNull", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of()), isZero);

        InstanceIntrinsic identity = (builder, instance, target, arguments) -> instance;

        intrinsics.registerIntrinsic(ptrDesc, "deref", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), identity);
        intrinsics.registerIntrinsic(ptrDesc, "asArray", MethodDescriptor.synthesize(classContext, ArrayTypeDescriptor.of(classContext, nObjDesc), List.of()), identity);

        InstanceIntrinsic get = (builder, instance, target, arguments) ->
            builder.load(builder.elementOf(builder.pointerHandle(instance), arguments.get(0)), MemoryAtomicityMode.NONE);

        intrinsics.registerIntrinsic(ptrDesc, "get", MethodDescriptor.synthesize(classContext, nObjDesc, List.of(BaseTypeDescriptor.I)), get);

        InstanceIntrinsic set = (builder, instance, target, arguments) -> {
            builder.store(builder.elementOf(builder.pointerHandle(instance), arguments.get(0)), arguments.get(1), MemoryAtomicityMode.NONE);
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getExecutable().getType().getReturnType());
        };

        intrinsics.registerIntrinsic(ptrDesc, "set", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(BaseTypeDescriptor.I, nObjDesc)), set);

        InstanceIntrinsic plus = (builder, instance, target, arguments) -> builder.addressOf(builder.elementOf(builder.pointerHandle(instance), arguments.get(0)));

        intrinsics.registerIntrinsic(ptrDesc, "plus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(BaseTypeDescriptor.I)), plus);
        intrinsics.registerIntrinsic(ptrDesc, "plus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(ptrDiffTDesc)), plus);
        intrinsics.registerIntrinsic(ptrDesc, "plus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(sizeTDesc)), plus);

        InstanceIntrinsic minus = (builder, instance, target, arguments) -> builder.addressOf(builder.elementOf(builder.pointerHandle(instance), builder.negate(arguments.get(0))));

        intrinsics.registerIntrinsic(ptrDesc, "minus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(BaseTypeDescriptor.I)), minus);
        intrinsics.registerIntrinsic(ptrDesc, "minus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(ptrDiffTDesc)), minus);
        intrinsics.registerIntrinsic(ptrDesc, "minus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(sizeTDesc)), minus);

        InstanceIntrinsic castToType = (builder, input, target, arguments) -> {
            Value arg0 = arguments.get(0);
            if (arg0 instanceof ClassOf) {
                Value typeLit = ((ClassOf) arg0).getInput();
                if (typeLit instanceof TypeLiteral) {
                    ValueType toType = ((TypeLiteral) typeLit).getValue();
                    if (toType instanceof WordType) {
                        return smartConvert(builder, input, (WordType) toType, false);
                    } else {
                        return input;
                    }
                }
            }
            ctxt.error(builder.getLocation(), "Expected class literal as argument to cast");
            return input;
        };

        intrinsics.registerIntrinsic(nObjDesc, "cast", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), identity);
        intrinsics.registerIntrinsic(nObjDesc, "cast", MethodDescriptor.synthesize(classContext, nObjDesc, List.of(classDesc)), castToType);

        StaticIntrinsic alloca = (builder, target, arguments) -> builder.stackAllocate(ctxt.getTypeSystem().getUnsignedInteger8Type(), arguments.get(0), ctxt.getLiteralFactory().literalOf(1));

        intrinsics.registerIntrinsic(cNativeDesc, "alloca", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(sizeTDesc)), alloca);

        StaticIntrinsic identityStatic = (builder, target, arguments) -> arguments.get(0);

        intrinsics.registerIntrinsic(cNativeDesc, "word", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.Z)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "word", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.I)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "word", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.J)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "word", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.F)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "word", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.D)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "word", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.Z)), identityStatic);

        StaticIntrinsic toUnsigned = (builder, target, arguments) ->
            builder.bitCast(arguments.get(0), ((IntegerType)arguments.get(0).getType()).asUnsigned());

        intrinsics.registerIntrinsic(cNativeDesc, "uword", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.I)), toUnsigned);
        intrinsics.registerIntrinsic(cNativeDesc, "uword", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.J)), toUnsigned);

        StaticIntrinsic sizeof = (builder, target, arguments) -> {
            long size = arguments.get(0).getType().getSize();
            IntegerType returnType = (IntegerType) target.getExecutable().getType().getReturnType();
            return ctxt.getLiteralFactory().literalOf(returnType, size);
        };

        StaticIntrinsic sizeofClass = (builder, target, arguments) -> {
            Value arg = arguments.get(0);
            long size;
            /* Class should be ClassOf(TypeLiteral) */
            if (arg instanceof ClassOf && ((ClassOf) arg).getInput() instanceof TypeLiteral) {
                TypeLiteral input = (TypeLiteral) (((ClassOf) arg).getInput());
                size = input.getValue().getSize();
            } else {
                ctxt.error(builder.getLocation(), "unexpected type for sizeof(Class)");
                size = arg.getType().getSize();
            }
            IntegerType returnType = (IntegerType) target.getExecutable().getType().getReturnType();
            return ctxt.getLiteralFactory().literalOf(returnType, size);
        };

        intrinsics.registerIntrinsic(cNativeDesc, "sizeof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(nObjDesc)), sizeof);
        intrinsics.registerIntrinsic(cNativeDesc, "sizeof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(ArrayTypeDescriptor.of(classContext, nObjDesc))), sizeof);
        intrinsics.registerIntrinsic(cNativeDesc, "sizeof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(classDesc)), sizeofClass);
        intrinsics.registerIntrinsic(cNativeDesc, "sizeofArray", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(ArrayTypeDescriptor.of(classContext, classDesc))), sizeofClass);

        StaticIntrinsic alignof = (builder, target, arguments) -> {
            ValueType argType = arguments.get(0).getType();
            long align;
            if (argType instanceof TypeType) {
                align = ((TypeType) argType).getUpperBound().getAlign();
            } else {
                align = argType.getAlign();
            }
            IntegerType returnType = (IntegerType) target.getExecutable().getType().getReturnType();
            return ctxt.getLiteralFactory().literalOf(returnType, align);
        };

        intrinsics.registerIntrinsic(cNativeDesc, "alignof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(nObjDesc)), alignof);
        intrinsics.registerIntrinsic(cNativeDesc, "alignof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(classDesc)), alignof);

        StaticIntrinsic defined = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(! (arguments.get(0) instanceof UndefinedLiteral));

        intrinsics.registerIntrinsic(cNativeDesc, "defined", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(nObjDesc)), defined);

        StaticIntrinsic isComplete = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(arguments.get(0).getType().isComplete());

        intrinsics.registerIntrinsic(cNativeDesc, "isComplete", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(nObjDesc)), isComplete);

        StaticIntrinsic isSigned = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(arguments.get(0).getType() instanceof SignedIntegerType);

        intrinsics.registerIntrinsic(cNativeDesc, "isSigned", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(nObjDesc)), isSigned);

        StaticIntrinsic isUnsigned = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(arguments.get(0).getType() instanceof UnsignedIntegerType);

        intrinsics.registerIntrinsic(cNativeDesc, "isUnsigned", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(nObjDesc)), isUnsigned);

        StaticIntrinsic typesAreEquivalent = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(arguments.get(0).getType().equals(arguments.get(1).getType()));

        intrinsics.registerIntrinsic(cNativeDesc, "typesAreEquivalent", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, Collections.nCopies(2, nObjDesc)), typesAreEquivalent);

        StaticIntrinsic zero = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(0);

        intrinsics.registerIntrinsic(cNativeDesc, "zero", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), zero);

        // todo: implement an "uninitialized" constant similar to zero
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), zero);

        StaticIntrinsic constant = (builder, target, arguments) ->
            ctxt.getLiteralFactory().constantLiteralOfType(ctxt.getTypeSystem().getPoisonType());

        intrinsics.registerIntrinsic(cNativeDesc, "constant", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), constant);

        StaticIntrinsic bitCast = (builder, target, arguments) ->
            builder.bitCast(arguments.get(0), (WordType) target.getExecutable().getType().getReturnType());

        intrinsics.registerIntrinsic(cNativeDesc, "ptrToRef", MethodDescriptor.synthesize(classContext, objDesc, List.of(ptrDesc)), bitCast);
        intrinsics.registerIntrinsic(cNativeDesc, "refToPtr", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(objDesc)), bitCast);
    }

    static void registerOrgQbiccObjectModelIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        CoreClasses coreClasses = CoreClasses.get(ctxt);
        SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
        LiteralFactory lf = ctxt.getLiteralFactory();

        ClassTypeDescriptor objModDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/ObjectModel");
        ClassTypeDescriptor typeIdDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$type_id");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor clsDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor jlsDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor uint8Desc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$uint8_t");
        ClassTypeDescriptor pthreadMutexPtrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/posix/PThread$pthread_mutex_t_ptr");
        ClassTypeDescriptor valsDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/Values");
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
        MethodDescriptor createClassDesc = MethodDescriptor.synthesize(classContext, clsDesc, List.of(jlsDesc, typeIdDesc, uint8Desc));
        MethodDescriptor clsClsDesc = MethodDescriptor.synthesize(classContext, clsDesc, List.of(clsDesc));
        MethodDescriptor clsClsBooleanDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(clsDesc, clsDesc));
        MethodDescriptor casDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, Collections.nCopies(3, BaseTypeDescriptor.J));

        StaticIntrinsic typeOf = (builder, target, arguments) ->
            builder.typeIdOf(builder.referenceHandle(arguments.get(0)));
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "type_id_of", objTypeIdDesc, typeOf);

        FieldElement elementTypeField = coreClasses.getRefArrayElementTypeIdField();
        StaticIntrinsic elementTypeOf = (builder, target, arguments) -> {
            ValueHandle handle = builder.referenceHandle(builder.bitCast(arguments.get(0), elementTypeField.getEnclosingType().load().getType().getReference()));
            return builder.load(builder.instanceFieldOf(handle, elementTypeField), MemoryAtomicityMode.UNORDERED);
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "element_type_id_of", objTypeIdDesc, elementTypeOf);

        FieldElement dimensionsField = coreClasses.getRefArrayDimensionsField();
        StaticIntrinsic dimensionsOf = (builder, target, arguments) -> {
            ValueHandle handle = builder.referenceHandle(builder.bitCast(arguments.get(0), dimensionsField.getEnclosingType().load().getType().getReference()));
            return builder.load(builder.instanceFieldOf(handle, dimensionsField), MemoryAtomicityMode.UNORDERED);
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "dimensions_of", objUint8Desc, dimensionsOf);

        StaticIntrinsic maxSubclassId = (builder, target, arguments) -> {
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            ValueHandle typeIdStruct = builder.elementOf(builder.globalVariable(typeIdGlobal), arguments.get(0));
            return builder.load(builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("maxSubTypeId")), MemoryAtomicityMode.UNORDERED);
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "max_subclass_type_id_of", typeIdTypeIdDesc, maxSubclassId);

        StaticIntrinsic isObject = (builder, target, arguments) -> {
            LoadedTypeDefinition jlo = classContext.findDefinedType("java/lang/Object").load();
            return builder.isEq(arguments.get(0), lf.literalOfType(jlo.getType()));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_java_lang_object", typeIdBooleanDesc, isObject);

        StaticIntrinsic isCloneable = (builder, target, arguments) -> {
            LoadedTypeDefinition jlc = classContext.findDefinedType("java/lang/Cloneable").load();
            return builder.isEq(arguments.get(0), lf.literalOfType(jlc.getType()));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_java_lang_cloneable", typeIdBooleanDesc, isCloneable);

        StaticIntrinsic isSerializable = (builder, target, arguments) -> {
            LoadedTypeDefinition jis = classContext.findDefinedType("java/io/Serializable").load();
            return builder.isEq(arguments.get(0), lf.literalOfType(jis.getType()));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_java_io_serializable", typeIdBooleanDesc, isSerializable);

        StaticIntrinsic isClass = (builder, target, arguments) -> {
            LoadedTypeDefinition jlo = classContext.findDefinedType("java/lang/Object").load();
            ValueType refArray = coreClasses.getArrayLoadedTypeDefinition("[ref").getType();
            Value isObj = builder.isEq(arguments.get(0), lf.literalOfType(jlo.getType()));
            Value isAboveRef = builder.isLt(lf.literalOfType(refArray), arguments.get(0));
            Value isNotInterface = builder.isLt(arguments.get(0), lf.literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), tables.getFirstInterfaceTypeId()));
            return builder.or(isObj, builder.and(isAboveRef, isNotInterface));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_class", typeIdBooleanDesc, isClass);

        StaticIntrinsic isInterface = (builder, target, arguments) ->
            builder.isLe(lf.literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), tables.getFirstInterfaceTypeId()), arguments.get(0));

        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_interface", typeIdBooleanDesc, isInterface);

        StaticIntrinsic isPrimArray = (builder, target, arguments) -> {
            ValueType firstPrimArray = coreClasses.getArrayLoadedTypeDefinition("[Z").getType();
            ValueType lastPrimArray = coreClasses.getArrayLoadedTypeDefinition("[D").getType();
            return builder.and(builder.isGe(arguments.get(0), lf.literalOfType(firstPrimArray)),
                builder.isLe(arguments.get(0), lf.literalOfType(lastPrimArray)));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_prim_array", typeIdBooleanDesc, isPrimArray);

        StaticIntrinsic isPrimitive = (builder, target, arguments) -> {
            ValueType firstPrimType = Primitive.getPrimitiveFor('Z').getType();
            ValueType lastPrimType = Primitive.getPrimitiveFor('V').getType();
            return builder.and(builder.isGe(arguments.get(0), lf.literalOfType(firstPrimType)),
                               builder.isLe(arguments.get(0), lf.literalOfType(lastPrimType)));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_primitive", typeIdBooleanDesc, isPrimitive);

        StaticIntrinsic isRefArray = (builder, target, arguments) -> {
            ValueType refArray = coreClasses.getArrayLoadedTypeDefinition("[ref").getType();
            return builder.isEq(arguments.get(0), lf.literalOfType(refArray));
        };

        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_reference_array", typeIdBooleanDesc, isRefArray);

        StaticIntrinsic getRefArrayTypeId = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), coreClasses.getRefArrayContentField().getEnclosingType().load().getTypeId());
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_reference_array_typeid", emptyTotypeIdDesc, getRefArrayTypeId);

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
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "does_implement", typeIdTypeIdBooleanDesc, doesImplement);

        StaticIntrinsic getDimFromClass = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), coreClasses.getClassDimensionField()), MemoryAtomicityMode.UNORDERED);
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_dimensions_from_class", clsUint8, getDimFromClass);

        StaticIntrinsic getTypeIdFromClass = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), coreClasses.getClassTypeIdField()), MemoryAtomicityMode.UNORDERED);
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_type_id_from_class", clsTypeId, getTypeIdFromClass);

        MethodElement getOrCreateArrayClass = ctxt.getOMHelperMethod("get_or_create_class_for_refarray");
        StaticIntrinsic getClassFromTypeId = (builder, target, arguments) -> {
            /** Pseudo code for this intrinsic:
             *    Class<?> componentClass = qbicc_jlc_lookup_table[typeId];
             *    Class<?> result = componentClass;
             *    if (dims > 0) {
             *        result = get_or_create_class_for_refarray(componentClass, dims);
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
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_class_from_type_id", typeIdClsDesc, getClassFromTypeId);

        StaticIntrinsic getClassFromTypeIdSimple = (builder, target, arguments) -> {
            BuildtimeHeap buildtimeHeap = BuildtimeHeap.get(ctxt);
            GlobalVariableElement classArrayGlobal = buildtimeHeap.getAndRegisterGlobalClassArray(builder.getCurrentElement());
            // todo: if this is changed from load to referenceTo, also delete isConstant from ClassOf and fix it in getClassFromTypeId
            return builder.load(builder.elementOf(builder.globalVariable(classArrayGlobal), arguments.get(0)), MemoryAtomicityMode.UNORDERED);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_class_from_type_id_simple", typeIdToClassDesc, getClassFromTypeIdSimple);

        StaticIntrinsic getArrayClassOf = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), CoreClasses.get(ctxt).getArrayClassField()), MemoryAtomicityMode.UNORDERED);
        intrinsics.registerIntrinsic(objModDesc, "get_array_class_of", clsClsDesc, getArrayClassOf);

        StaticIntrinsic setArrayClass = (builder, target, arguments) -> {
            LoadedTypeDefinition jlc = classContext.findDefinedType("java/lang/Class").load();
            Value expr = builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), CoreClasses.get(ctxt).getArrayClassField()), MemoryAtomicityMode.UNORDERED);
            Value expect = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(jlc.getType().getReference());
            Value update = arguments.get(1);
            ValueHandle valuesCompareAndSwap = builder.staticMethod(valsDesc, "compareAndSwap", casDesc);
            return builder.call(valuesCompareAndSwap, List.of(expr, expect, update));
        };
        intrinsics.registerIntrinsic(objModDesc, "set_array_class", clsClsBooleanDesc, setArrayClass);


        FieldElement jlcName = classContext.findDefinedType("java/lang/Class").load().findField("name");

        StaticIntrinsic createClass = (builder, target, arguments) -> {
            ClassObjectType jlcType = (ClassObjectType) ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load().getType();
            Value instance = builder.new_(jlcType);
            ValueHandle instanceHandle = builder.referenceHandle(instance);
            ValueHandle handle = builder.instanceFieldOf(instanceHandle, jlcName);
            builder.store(handle, arguments.get(0), handle.getDetectedMode());
            handle = builder.instanceFieldOf(instanceHandle, CoreClasses.get(ctxt).getClassTypeIdField());
            builder.store(handle, arguments.get(1), handle.getDetectedMode());
            handle = builder.instanceFieldOf(builder.referenceHandle(instance), CoreClasses.get(ctxt).getClassDimensionField());
            builder.store(handle, arguments.get(2), handle.getDetectedMode());
            return instance;
        };
        intrinsics.registerIntrinsic(Phase.ADD, objModDesc, "create_class", createClassDesc, createClass);

        StaticIntrinsic getNumberOfTypeIds = (builder, target, arguments) -> lf.literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), tables.get_number_of_typeids());
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_number_of_typeids", emptyTotypeIdDesc, getNumberOfTypeIds);

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

            return builder.call(builder.pointerHandle(typeIdInit), List.of(builder.currentThread()));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "call_class_initializer", typeIdVoidDesc, callClassInitializer);

        // int get_typeid_flags(CNative.type_id typeID);
        StaticIntrinsic get_typeid_flags = (builder, target, arguments) -> {
            Value typeId = arguments.get(0);
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            ValueHandle typeIdStruct = builder.elementOf(builder.globalVariable(typeIdGlobal), typeId);
            ValueHandle flags = builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("flags"));
            Value flagValue = builder.load(flags, MemoryAtomicityMode.UNORDERED);
            return flagValue;
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_typeid_flags", typeIdIntDesc, get_typeid_flags);

        // public static native CNative.type_id get_superclass_typeid(CNative.type_id typeId);
        StaticIntrinsic get_superclass_typeid = (builder, target, arguments) -> {
            Value typeId = arguments.get(0);
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            ValueHandle typeIdStruct = builder.elementOf(builder.globalVariable(typeIdGlobal), typeId);
            ValueHandle superTypeId = builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("superTypeId"));
            Value superTypeIdValue = builder.load(superTypeId, MemoryAtomicityMode.UNORDERED);
            return superTypeIdValue;
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_superclass_typeid", typeIdTypeIdDesc, get_superclass_typeid);

        // public static native CNative.type_id get_first_interface_typeid();
        StaticIntrinsic get_first_interface_typeid = (builder, target, arguments) -> {
            return lf.literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), tables.getFirstInterfaceTypeId());
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_first_interface_typeid", emptyTotypeIdDesc, get_first_interface_typeid);

        // public static native int get_number_of_bytes_in_interface_bits_array();
        StaticIntrinsic get_number_of_bytes_in_interface_bits_array = (builder, target, arguments) -> {
            return lf.literalOf(tables.getNumberOfBytesInInterfaceBitsArray());
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_number_of_bytes_in_interface_bits_array", IntDesc, get_number_of_bytes_in_interface_bits_array);

        // public static native byte get_byte_of_interface_bits(CNative.type_id typeId, int index);
        StaticIntrinsic get_byte_of_interface_bits = (builder, target, arguments) -> {
            Value typeId = arguments.get(0);
            Value index = arguments.get(1);
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            ValueHandle typeIdStruct = builder.elementOf(builder.globalVariable(typeIdGlobal), typeId);
            ValueHandle bits = builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("interfaceBits"));
            Value dataByte = builder.load(builder.elementOf(bits, index), MemoryAtomicityMode.UNORDERED);
            return dataByte;
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_byte_of_interface_bits", typeIdIntToByteDesc, get_byte_of_interface_bits);

        // public static native boolean is_initialized(CNative.type_id typdId);
        StaticIntrinsic is_initialized = (builder, target, arguments) -> {
            Value typeId = arguments.get(0);

            GlobalVariableElement clinitStates = tables.getAndRegisterGlobalClinitStateStruct(builder.getCurrentElement());
            CompoundType clinitStates_t = (CompoundType) clinitStates.getType();
            ValueHandle init_state_array = builder.memberOf(builder.globalVariable(clinitStates), clinitStates_t.getMember("init_state"));
            Value state = builder.load(builder.elementOf(init_state_array, typeId), MemoryAtomicityMode.ACQUIRE);

            return builder.isEq(state, lf.literalOf(1));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_initialized", typeIdBooleanDesc, is_initialized);

        // public static native void set_initialized(CNative.type_id typdId);
        StaticIntrinsic set_initialized = (builder, target, arguments) -> {
            Value typeId = arguments.get(0);

            GlobalVariableElement clinitStates = tables.getAndRegisterGlobalClinitStateStruct(builder.getCurrentElement());
            CompoundType clinitStates_t = (CompoundType) clinitStates.getType();
            Member init_state_t = clinitStates_t.getMember("init_state");
            IntegerType init_state_element_t = (IntegerType)((ArrayType)clinitStates_t.getMember("init_state").getType()).getElementType();
            ValueHandle init_state_array = builder.memberOf(builder.globalVariable(clinitStates), init_state_t);
            builder.store(builder.elementOf(init_state_array, typeId), lf.literalOf(init_state_element_t, 1), MemoryAtomicityMode.RELEASE);

            return lf.zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());

        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "set_initialized", typeIdVoidDesc, set_initialized);

        FieldElement nativeObjectMonitorField = CoreClasses.get(ctxt).getObjectNativeObjectMonitorField();
        // PThread.pthread_mutex_t_ptr get_nativeObjectMonitor(Object reference);
        MethodDescriptor nomOfDesc = MethodDescriptor.synthesize(classContext, pthreadMutexPtrDesc, List.of(objDesc));
        StaticIntrinsic nomOf = (builder, target, arguments) -> {
            Value mutexSlot = builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), nativeObjectMonitorField), MemoryAtomicityMode.NONE);
            PointerType returnType = (PointerType)target.getExecutable().getType().getReturnType();
            return builder.valueConvert(mutexSlot, returnType);
        };
        intrinsics.registerIntrinsic(objModDesc, "get_nativeObjectMonitor", nomOfDesc, nomOf);

        // boolean set_nativeObjectMonitor(Object object, PThread.pthread_mutex_t_ptr nom);
        MethodDescriptor setNomDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(objDesc, pthreadMutexPtrDesc));
        StaticIntrinsic setNom = (builder, target, arguments) -> {
            Value expr = builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), nativeObjectMonitorField), MemoryAtomicityMode.NONE);
            Value expect = ctxt.getLiteralFactory().literalOf(0L);
            Value update = builder.valueConvert(arguments.get(1), (SignedIntegerType)nativeObjectMonitorField.getType());
            ValueHandle valuesCompareAndSwap = builder.staticMethod(valsDesc, "compareAndSwap", casDesc);
            return builder.call(valuesCompareAndSwap, List.of(expr, expect, update));
        };
        intrinsics.registerIntrinsic(objModDesc, "set_nativeObjectMonitor", setNomDesc, setNom);
    }

    static void registerOrgQbiccRuntimeValuesIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        // descriptors

        ClassTypeDescriptor valsDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/Values");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");

        MethodDescriptor objBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(objDesc));
        MethodDescriptor boolBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.Z));
        MethodDescriptor longBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.J));
        MethodDescriptor intBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.I));
        MethodDescriptor shortBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.S));
        MethodDescriptor byteBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.B));
        MethodDescriptor charBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.C));
        MethodDescriptor floatBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.F));
        MethodDescriptor doubleBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.D));

        MethodDescriptor objObjObjDescriptor = MethodDescriptor.synthesize(classContext, objDesc, Collections.nCopies(2, objDesc));
        MethodDescriptor longLongLongDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, Collections.nCopies(2, BaseTypeDescriptor.J));
        MethodDescriptor intIntIntDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, Collections.nCopies(2, BaseTypeDescriptor.I));

        MethodDescriptor objObjVoidDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, Collections.nCopies(2, objDesc));
        MethodDescriptor longLongVoidDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, Collections.nCopies(2, BaseTypeDescriptor.J));
        MethodDescriptor intIntVoidDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, Collections.nCopies(2, BaseTypeDescriptor.I));

        MethodDescriptor objObjDescriptor = MethodDescriptor.synthesize(classContext, objDesc, List.of(objDesc));
        MethodDescriptor longLongDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.J));
        MethodDescriptor intIntDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.J));

        MethodDescriptor boolObjObjObjDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, Collections.nCopies(3, objDesc));
        MethodDescriptor boolLongLongLongDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, Collections.nCopies(3, BaseTypeDescriptor.J));
        MethodDescriptor boolIntIntIntDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, Collections.nCopies(3, BaseTypeDescriptor.I));

        // isConstant

        StaticIntrinsic isConstant = (builder, target, arguments) -> literalOf(ctxt, arguments.get(0) instanceof Literal);

        intrinsics.registerIntrinsic(valsDesc, "isConstant", objBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", boolBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", longBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", intBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", shortBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", byteBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", charBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", floatBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", doubleBoolDesc, isConstant);

        // isAlways*

        StaticIntrinsic isAlwaysTrue = (builder, target, arguments) -> literalOf(ctxt, arguments.get(0) instanceof BooleanLiteral && ((BooleanLiteral) arguments.get(0)).booleanValue());
        intrinsics.registerIntrinsic(valsDesc, "isAlwaysTrue", boolBoolDesc, isAlwaysTrue);

        StaticIntrinsic isAlwaysFalse = (builder, target, arguments) -> literalOf(ctxt, arguments.get(0) instanceof BooleanLiteral && ((BooleanLiteral) arguments.get(0)).booleanValue());
        intrinsics.registerIntrinsic(valsDesc, "isAlwaysFalse", boolBoolDesc, isAlwaysFalse);


        // compareAndSwap*
        class CompareAndSwapIntrinsic implements StaticIntrinsic {
            private final MemoryAtomicityMode successMode;
            private final MemoryAtomicityMode failureMode;
            CompareAndSwapIntrinsic(final MemoryAtomicityMode successMode, final MemoryAtomicityMode failureMode) {
                this.successMode = successMode;
                this.failureMode = failureMode;
            }

            @Override
            public Value emitIntrinsic(BasicBlockBuilder builder, StaticMethodElementHandle element, List<Value> arguments) {
                ValueHandle target = getTarget(ctxt, builder, arguments.get(0));
                if (target == null) {
                    return ctxt.getLiteralFactory().literalOf(false);
                }
                Value expect = arguments.get(1);
                Value update = arguments.get(2);
                Value result = builder.cmpAndSwap(target, expect, update, successMode, failureMode, CmpAndSwap.Strength.STRONG);
                Value resultValue = builder.extractMember(result, ((CmpAndSwap)result).getResultValueType());
                /* set was successful when expected value is returned */
                return builder.isEq(resultValue, expect);
            }
        }

        StaticIntrinsic compareAndSwapVolatile = new CompareAndSwapIntrinsic(MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT, MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
        StaticIntrinsic compareAndSwapAcquire = new CompareAndSwapIntrinsic(MemoryAtomicityMode.ACQUIRE, MemoryAtomicityMode.MONOTONIC);
        StaticIntrinsic compareAndSwapRelease = new CompareAndSwapIntrinsic(MemoryAtomicityMode.RELEASE, MemoryAtomicityMode.MONOTONIC);
        StaticIntrinsic compareAndSwap = new CompareAndSwapIntrinsic(MemoryAtomicityMode.MONOTONIC, MemoryAtomicityMode.MONOTONIC);
        intrinsics.registerIntrinsic(valsDesc, "compareAndSwapVolatile", boolObjObjObjDescriptor, compareAndSwapVolatile);
        intrinsics.registerIntrinsic(valsDesc, "compareAndSwapVolatile",  boolIntIntIntDescriptor, compareAndSwapVolatile);
        intrinsics.registerIntrinsic(valsDesc, "compareAndSwapVolatile", boolLongLongLongDescriptor, compareAndSwapVolatile);
        intrinsics.registerIntrinsic(valsDesc, "compareAndSwapAcquire", boolObjObjObjDescriptor, compareAndSwapAcquire);
        intrinsics.registerIntrinsic(valsDesc, "compareAndSwapAcquire", boolIntIntIntDescriptor, compareAndSwapAcquire);
        intrinsics.registerIntrinsic(valsDesc, "compareAndSwapAcquire", boolLongLongLongDescriptor, compareAndSwapAcquire);
        intrinsics.registerIntrinsic(valsDesc, "compareAndSwapRelease", boolObjObjObjDescriptor, compareAndSwapRelease);
        intrinsics.registerIntrinsic(valsDesc, "compareAndSwapRelease", boolIntIntIntDescriptor, compareAndSwapRelease);
        intrinsics.registerIntrinsic(valsDesc, "compareAndSwapRelease", boolLongLongLongDescriptor, compareAndSwapRelease);
        intrinsics.registerIntrinsic(valsDesc, "compareAndSwap", boolObjObjObjDescriptor, compareAndSwap);
        intrinsics.registerIntrinsic(valsDesc, "compareAndSwap", boolIntIntIntDescriptor, compareAndSwap);
        intrinsics.registerIntrinsic(valsDesc, "compareAndSwap", boolLongLongLongDescriptor, compareAndSwap);

        class GetAndSetIntrinsic implements StaticIntrinsic {
            private final MemoryAtomicityMode mode;

            GetAndSetIntrinsic(MemoryAtomicityMode mode) {
                this.mode = mode;
            }

            @Override
            public Value emitIntrinsic(BasicBlockBuilder builder, StaticMethodElementHandle element, List<Value> arguments) {
                ValueHandle target = getTarget(ctxt, builder, arguments.get(0));
                if (target == null) {
                    return arguments.get(0);
                }
                return builder.getAndSet(target, arguments.get(1), mode);
            }
        }

        StaticIntrinsic getAndSetVolatile = new GetAndSetIntrinsic(MemoryAtomicityMode.VOLATILE);

        intrinsics.registerIntrinsic(valsDesc, "getAndSetVolatile", objObjObjDescriptor, getAndSetVolatile);
        intrinsics.registerIntrinsic(valsDesc, "getAndSetVolatile", longLongLongDescriptor, getAndSetVolatile);
        intrinsics.registerIntrinsic(valsDesc, "getAndSetVolatile", intIntIntDescriptor, getAndSetVolatile);

        // todo: determine the real atomicity mode for "relaxed"
        StaticIntrinsic getAndSetRelaxed = new GetAndSetIntrinsic(MemoryAtomicityMode.MONOTONIC);

        intrinsics.registerIntrinsic(valsDesc, "getAndSetRelaxed", objObjObjDescriptor, getAndSetRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "getAndSetRelaxed", longLongLongDescriptor, getAndSetRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "getAndSetRelaxed", intIntIntDescriptor, getAndSetRelaxed);

        // set*

        Literal voidLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());

        class SetIntrinsic implements StaticIntrinsic {
            private final MemoryAtomicityMode mode;

            SetIntrinsic(MemoryAtomicityMode mode) {
                this.mode = mode;
            }

            @Override
            public Value emitIntrinsic(BasicBlockBuilder builder, StaticMethodElementHandle element, List<Value> arguments) {
                ValueHandle target = getTarget(ctxt, builder, arguments.get(0));
                if (target == null) {
                    builder.nop();
                } else {
                    builder.store(target, arguments.get(1), mode);
                }
                return voidLiteral;
            }
        }

        StaticIntrinsic setVolatile = new SetIntrinsic(MemoryAtomicityMode.VOLATILE);

        intrinsics.registerIntrinsic(valsDesc, "setVolatile", objObjVoidDescriptor, setVolatile);
        intrinsics.registerIntrinsic(valsDesc, "setVolatile", intIntVoidDescriptor, setVolatile);
        intrinsics.registerIntrinsic(valsDesc, "setVolatile", longLongVoidDescriptor, setVolatile);

        // todo: determine the real atomicity mode for "relaxed"
        StaticIntrinsic setRelaxed = new SetIntrinsic(MemoryAtomicityMode.MONOTONIC);

        intrinsics.registerIntrinsic(valsDesc, "setRelaxed", objObjVoidDescriptor, setRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "setRelaxed", intIntVoidDescriptor, setRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "setRelaxed", longLongVoidDescriptor, setRelaxed);

        // todo: exitConstructorBarrier

        // get*

        class GetIntrinsic implements StaticIntrinsic {
            private final MemoryAtomicityMode mode;

            GetIntrinsic(MemoryAtomicityMode mode) {
                this.mode = mode;
            }

            @Override
            public Value emitIntrinsic(BasicBlockBuilder builder, StaticMethodElementHandle element, List<Value> arguments) {
                ValueHandle target = getTarget(ctxt, builder, arguments.get(0));
                if (target == null) {
                    return arguments.get(0);
                }
                return builder.load(target, mode);
            }
        }

        StaticIntrinsic getVolatile = new GetIntrinsic(MemoryAtomicityMode.VOLATILE);

        intrinsics.registerIntrinsic(valsDesc, "getVolatile", objObjDescriptor, getVolatile);
        intrinsics.registerIntrinsic(valsDesc, "getVolatile", intIntDescriptor, getVolatile);
        intrinsics.registerIntrinsic(valsDesc, "getVolatile", longLongDescriptor, getVolatile);

        // todo: determine the real atomicity mode for "relaxed"
        StaticIntrinsic getRelaxed = new GetIntrinsic(MemoryAtomicityMode.MONOTONIC);

        intrinsics.registerIntrinsic(valsDesc, "getRelaxed", objObjDescriptor, getRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "getRelaxed", intIntDescriptor, getRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "getRelaxed", objObjDescriptor, getRelaxed);
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
                    ctxt.error(builder.getLocation(), "Ambiguous target for operation; to target the pointer value, use deref(val); to target the variable use addr_of(val)");
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

    static Value smartConvert(BasicBlockBuilder builder, Value input, WordType toType, boolean cRules) {
        CompilationContext ctxt = builder.getCurrentElement().getEnclosingType().getContext().getCompilationContext();
        ValueType fromType = input.getType();
        // work out the behavior based on input and output types
        if (toType instanceof BooleanType) {
            if (fromType instanceof BooleanType) {
                return input;
            } else if (cRules) {
                // in this case we want != 0 behavior like C
                return builder.isNe(input, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(input.getType()));
            } else {
                // in this case we want bit cast behavior
                return builder.truncate(input, toType);
            }
        } else if (toType instanceof IntegerType) {
            if (fromType instanceof IntegerType) {
                IntegerType inputType = (IntegerType) fromType;
                if (toType.getMinBits() > inputType.getMinBits()) {
                    return builder.extend(input, toType);
                } else if (toType.getMinBits() < inputType.getMinBits()) {
                    return builder.truncate(input, toType);
                } else {
                    return builder.bitCast(input, toType);
                }
            } else if (fromType instanceof WordType) {
                return builder.valueConvert(input, toType);
            } else {
                return input;
            }
        } else if (toType instanceof FloatType) {
            if (fromType instanceof FloatType) {
                FloatType inputType = (FloatType) fromType;
                if (toType.getMinBits() > inputType.getMinBits()) {
                    return builder.extend(input, toType);
                } else if (toType.getMinBits() < inputType.getMinBits()) {
                    return builder.truncate(input, toType);
                } else {
                    return input;
                }
            } else if (fromType instanceof WordType) {
                return builder.valueConvert(input, toType);
            } else {
                return input;
            }
        } else if (toType instanceof PointerType) {
            if (fromType instanceof PointerType) {
                return builder.bitCast(input, toType);
            } else if (fromType instanceof WordType) {
                return builder.valueConvert(input, toType);
            } else {
                return input;
            }
        } else {
            return builder.valueConvert(input, toType);
        }
    }

    private static void registerJavaLangReflectIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jlcDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor jloDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor arrayClassDescriptor = ClassTypeDescriptor.synthesize(classContext, "java/lang/reflect/Array");

        MethodDescriptor newArrayDesc = MethodDescriptor.synthesize(classContext, jloDesc, List.of(jlcDesc, BaseTypeDescriptor.I));
        StaticIntrinsic newArray = (builder, target, arguments) -> {
            // TODO: Real implementation of Array.newInstance(Class, int)
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(classContext.findDefinedType("java/lang/Object").load().getType().getReference());
        };


        MethodDescriptor multiNewArrayDesc = MethodDescriptor.synthesize(classContext, jloDesc, List.of(jlcDesc, ArrayTypeDescriptor.of (classContext, BaseTypeDescriptor.I)));
        StaticIntrinsic multiNewArray = (builder, target, arguments) -> {
            // TODO: Real implementation of Array.multiNewInstance(Class, int[])
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(classContext.findDefinedType("java/lang/Object").load().getType().getReference());
        };

        intrinsics.registerIntrinsic(Phase.LOWER, arrayClassDescriptor, "newArray", newArrayDesc, newArray);
        intrinsics.registerIntrinsic(Phase.LOWER, arrayClassDescriptor, "multiNewArray", multiNewArrayDesc, multiNewArray);
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

package org.qbicc.plugin.intrinsics.core;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.ProgramObject;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.plugin.dispatch.DispatchTables;
import org.qbicc.plugin.gc.nogc.NoGc;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayTables;
import org.qbicc.plugin.intrinsics.InstanceIntrinsic;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.plugin.intrinsics.StaticIntrinsic;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.pointer.ProgramObjectPointer;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.NullableType;
import org.qbicc.type.Primitive;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.StaticFieldElement;
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
        BuildIntrinsics.register(ctxt);
        CNativeIntrinsics.register(ctxt);
        registerJavaLangClassIntrinsics(ctxt);
        registerJavaLangStringUTF16Intrinsics(ctxt);
        registerJavaLangSystemIntrinsics(ctxt);
        registerJavaLangStackTraceElementIntrinsics(ctxt);
        registerJavaLangThreadIntrinsics(ctxt);
        registerJavaLangNumberIntrinsics(ctxt);
        registerJavaLangFloatDoubleMathIntrinsics(ctxt);
        registerJavaLangRefIntrinsics(ctxt);
        registerOrgQbiccCompilerIntrinsics(ctxt);
        registerOrgQbiccObjectModelIntrinsics(ctxt);
        registerOrgQbiccRuntimeBuildIntrinsics(ctxt);
        registerOrgQbiccRuntimeMainIntrinsics(ctxt);
        registerJavaLangMathIntrinsics(ctxt);
        registerJavaLangIntegerIntrinsics(ctxt);
        registerJavaLangLongIntrinsics(ctxt);
        registerJavaUtilConcurrentAtomicLongIntrinsics(ctxt);
        UnsafeIntrinsics.register(ctxt);
        registerJDKInternalIntrinsics(ctxt);
    }

    private static StaticIntrinsic setVolatile(CompilationContext ctxt, StaticFieldElement field) {
        Literal voidLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());
        return (builder, target, arguments) -> {
            builder.store(builder.getLiteralFactory().literalOf(field), arguments.get(0), GlobalSeqCst);
            return voidLiteral;
        };
    }

    public static void registerJavaLangClassIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jlcDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor jlsDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor jloDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");

        MethodDescriptor classToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(jlcDesc));
        MethodDescriptor emptyToObjArray = MethodDescriptor.synthesize(classContext, ArrayTypeDescriptor.of(classContext, jloDesc), List.of());
        MethodDescriptor emptyToClass = MethodDescriptor.synthesize(classContext, jlcDesc, List.of());
        MethodDescriptor emptyToString = MethodDescriptor.synthesize(classContext, jlsDesc, List.of());
        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());
        MethodDescriptor stringToClass = MethodDescriptor.synthesize(classContext, jlcDesc, List.of(jlsDesc));

        // Assertion status

        // todo: this probably belongs in the class libraries rather than here
        StaticIntrinsic desiredAssertionStatus0 = (builder, target, arguments) ->
            classContext.getLiteralFactory().literalOf(true);

        InstanceIntrinsic desiredAssertionStatus =  (builder, instance, target, arguments) ->
            classContext.getLiteralFactory().literalOf(true);

        InstanceIntrinsic initClassName = (builder, instance, target, arguments) -> {
            // not reachable; we always would initialize our class name eagerly
            throw new BlockEarlyTermination(builder.unreachable());
        };

        //    static native Class<?> getPrimitiveClass(String name);
        StaticIntrinsic getPrimitiveClass = (builder, target, arguments) -> {
            // always called with a string literal
            StringLiteral lit = (StringLiteral) arguments.get(0);
            LiteralFactory lf = ctxt.getLiteralFactory();
            TypeSystem ts = ctxt.getTypeSystem();
            ValueType type = Primitive.getPrimitiveFor(lit.getValue()).getType();
            return builder.classOf(lf.literalOfType(type), lf.zeroInitializerLiteralOfType(ts.getUnsignedInteger8Type()));
        };

        intrinsics.registerIntrinsic(jlcDesc, "desiredAssertionStatus0", classToBool, desiredAssertionStatus0);
        intrinsics.registerIntrinsic(jlcDesc, "desiredAssertionStatus", emptyToBool, desiredAssertionStatus);
        intrinsics.registerIntrinsic(jlcDesc, "initClassName", emptyToString, initClassName);
        intrinsics.registerIntrinsic(jlcDesc, "getPrimitiveClass", stringToClass, getPrimitiveClass);

        InstanceIntrinsic getEnclosingMethod0 = (builder, instance, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            return lf.nullLiteralOfType((NullableType) target.getReturnType());
        };

        intrinsics.registerIntrinsic(Phase.ANALYZE, jlcDesc, "getEnclosingMethod0", emptyToObjArray, getEnclosingMethod0);

        InstanceIntrinsic getDeclaringClass0 = (builder, instance, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            return lf.nullLiteralOfType((NullableType) target.getReturnType());
        };

        intrinsics.registerIntrinsic(Phase.ANALYZE, jlcDesc, "getDeclaringClass0", emptyToClass, getDeclaringClass0);
    }

    public static void registerJavaLangStringUTF16Intrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jlsu16Desc = ClassTypeDescriptor.synthesize(classContext, "java/lang/StringUTF16");

        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        StaticIntrinsic isBigEndian = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(ctxt.getTypeSystem().getEndianness() == ByteOrder.BIG_ENDIAN);

        intrinsics.registerIntrinsic(jlsu16Desc, "isBigEndian", emptyToBool, isBigEndian);
    }

    public static void registerJavaLangSystemIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor systemDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/System");
        LoadedTypeDefinition jls = classContext.findDefinedType("java/lang/System").load();

        // System public API

        StaticFieldElement in = jls.findStaticField("in");
        in.setModifierFlags(ClassFile.I_ACC_NOT_REALLY_FINAL);
        StaticFieldElement out = jls.findStaticField("out");
        out.setModifierFlags(ClassFile.I_ACC_NOT_REALLY_FINAL);
        StaticFieldElement err = jls.findStaticField("err");
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
    }

    public static void registerJavaLangThreadIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jltDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Thread");

        MethodDescriptor returnJlt = MethodDescriptor.synthesize(classContext, jltDesc, List.of());
        MethodDescriptor emptyToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());

        /* public static native Thread currentThread(); */
        StaticIntrinsic currentThread = (builder, target, arguments) -> builder.load(builder.currentThread(), SingleUnshared);
        intrinsics.registerIntrinsic(jltDesc, "currentThread", returnJlt, currentThread);

        //static native void bind(ptr<thread_native> threadPtr, Runnable r);
        StaticIntrinsic bindAndCallRun0 = (builder, targetPtr, arguments) ->
            builder.call(builder.threadBound(arguments.get(0), builder.resolveStaticMethod(jltDesc, "run0", emptyToVoid)), List.of(arguments.get(1)));
        intrinsics.registerIntrinsic(jltDesc, "bindAndCallRun0", bindAndCallRun0);
    }

    public static void registerJavaLangStackTraceElementIntrinsics(CompilationContext ctxt) {
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
            InstanceFieldElement backtraceField = jltVal.findInstanceField("backtrace");
            InstanceFieldElement depthField = jltVal.findInstanceField("depth");
            Value backtraceValue = builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(1)), backtraceField), SingleUnshared);
            Value depthValue = builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(1)), depthField), SingleUnshared);

            return builder.getFirstBuilder().call(builder.getLiteralFactory().literalOf(fillStackTraceElements), List.of(arguments.get(0), backtraceValue, depthValue));
        };

        intrinsics.registerIntrinsic(Phase.ANALYZE, steDesc, "initStackTraceElements", steArrayThrowableToVoidDesc, initStackTraceElements);
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

    static void registerOrgQbiccCompilerIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        CoreClasses coreClasses = CoreClasses.get(ctxt);

        ClassTypeDescriptor ciDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/CompilerIntrinsics");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor clsDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor strDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor voidUnaryfunctionPtrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$void_ptr_unaryoperator_function_ptr");

        MethodDescriptor nfpDesc = MethodDescriptor.synthesize(classContext, voidUnaryfunctionPtrDesc, List.of(strDesc, strDesc));
        StaticIntrinsic nfp = (builder, target, arguments) -> {
            StringLiteral clazz = (StringLiteral)arguments.get(0);
            StringLiteral name = (StringLiteral)arguments.get(1);
            LoadedTypeDefinition ltd = classContext.findDefinedType(clazz.getValue().replace('.', '/')).load();
            WordType toType = ctxt.getTypeSystem().getFunctionType(ctxt.getTypeSystem().getVoidType().getPointer(), List.of(ctxt.getTypeSystem().getVoidType().getPointer())).getPointer();
            try {
                MethodElement me = ltd.requireSingleMethod(name.getValue());
                // The three argument form of staticMethod enables the machinery that processes the @export annotation
                Value mh  = builder.resolveStaticMethod(me.getEnclosingType().getDescriptor(), name.getValue(), me.getDescriptor());
                return builder.bitCast(mh, toType);
            } catch (IllegalArgumentException e) {
                ctxt.error("CompilerIntrinsics.nativeFunctionPointer: failed to find: %s.%s", clazz.getValue(), name.getValue());
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(toType);
            }
        };
        intrinsics.registerIntrinsic(Phase.ADD, ciDesc, "nativeFunctionPointer", nfpDesc, nfp);

        MethodDescriptor newRefArrayDesc =  MethodDescriptor.synthesize(classContext, objDesc, List.of(clsDesc, BaseTypeDescriptor.I, BaseTypeDescriptor.I));
        StaticIntrinsic newRefArray = (builder, target, arguments) -> {
            ReferenceArrayObjectType upperBound = classContext.findDefinedType("java/lang/Object").load().getObjectType().getReferenceArrayObject();
            Value typeId = builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), coreClasses.getClassTypeIdField()));
            Value dims = builder.truncate(arguments.get(1), (WordType) coreClasses.getRefArrayDimensionsField().getType());
            return builder.newReferenceArray(upperBound, typeId, dims, arguments.get(2));
        };
        intrinsics.registerIntrinsic(Phase.ADD, ciDesc, "emitNewReferenceArray", newRefArrayDesc, newRefArray);

        MethodDescriptor newDesc = MethodDescriptor.synthesize(classContext, objDesc, List.of(clsDesc));
        StaticIntrinsic new_ = (builder, target, arguments) -> {
            ClassObjectType upperBound = classContext.findDefinedType("java/lang/Object").load().getClassType();
            Value typeId = builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), coreClasses.getClassTypeIdField()));
            Value instanceSize = builder.extend(builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), coreClasses.getClassInstanceSizeField())), ctxt.getTypeSystem().getSignedInteger64Type());
            Value instanceAlign = builder.extend(builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), coreClasses.getClassInstanceAlignField())), ctxt.getTypeSystem().getSignedInteger32Type());
            return builder.new_(upperBound, typeId, instanceSize, instanceAlign);
        };
        intrinsics.registerIntrinsic(Phase.ADD, ciDesc, "emitNew", newDesc, new_);

        MethodDescriptor copyDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(clsDesc, objDesc, objDesc));
        StaticIntrinsic copy = (builder, target, arguments) -> {
            Value cls = arguments.get(0);
            Value src = arguments.get(1);
            Value dst = arguments.get(2);
            Value size32 = builder.load(builder.instanceFieldOf(builder.decodeReference(cls), coreClasses.getClassInstanceSizeField()));
            Value size = builder.extend(size32, ctxt.getTypeSystem().getSignedInteger64Type());

            // TODO: This is a kludge in multiple ways:
            //  1. We should not directly call a NoGc method here.
            //  2. We are overwriting the object header fields initialized by new when doing the copy
            //     (to make sure we copy any instance fields that have been assigned to use the padding bytes in the basic object header).
            MethodElement method = NoGc.get(ctxt).getCopyMethod();
            return builder.call(builder.getLiteralFactory().literalOf(method), List.of(dst, src, size));
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

        MethodDescriptor objTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(objDesc));
        MethodDescriptor objUint8Desc = MethodDescriptor.synthesize(classContext, uint8Desc, List.of(objDesc));
        MethodDescriptor objIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(objDesc));
        MethodDescriptor typeIdTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(typeIdDesc));
        MethodDescriptor typeIdBooleanDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(typeIdDesc));
        MethodDescriptor typeIdTypeIdBooleanDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(typeIdDesc, typeIdDesc));
        MethodDescriptor intVoidDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(BaseTypeDescriptor.I));
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

        StaticIntrinsic typeOf = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), coreClasses.getObjectTypeIdField()));
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "typeIdOf", objTypeIdDesc, typeOf);

        InstanceFieldElement elementTypeField = coreClasses.getRefArrayElementTypeIdField();
        StaticIntrinsic elementTypeOf = (builder, target, arguments) -> {
            Value handle = builder.decodeReference(builder.bitCast(arguments.get(0), elementTypeField.getEnclosingType().load().getObjectType().getReference()));
            return builder.load(builder.instanceFieldOf(handle, elementTypeField));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "elementTypeIdOf", objTypeIdDesc, elementTypeOf);

        InstanceFieldElement dimensionsField = coreClasses.getRefArrayDimensionsField();
        StaticIntrinsic dimensionsOf = (builder, target, arguments) -> {
            Value handle = builder.decodeReference(builder.bitCast(arguments.get(0), dimensionsField.getEnclosingType().load().getObjectType().getReference()));
            return builder.load(builder.instanceFieldOf(handle, dimensionsField));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "dimensionsOf", objUint8Desc, dimensionsOf);

        InstanceFieldElement lengthField = coreClasses.getArrayLengthField();
        StaticIntrinsic lengthOf = (builder, target, arguments) -> {
            Value handle = builder.decodeReference(builder.bitCast(arguments.get(0), lengthField.getEnclosingType().load().getObjectType().getReference()));
            return builder.load(builder.instanceFieldOf(handle, lengthField));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "lengthOf", objIntDesc, lengthOf);

        StaticIntrinsic maxSubClassId = (builder, target, arguments) -> {
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            Value typeIdStruct = builder.elementOf(builder.getLiteralFactory().literalOf(typeIdGlobal), arguments.get(0));
            return builder.load(builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("maxSubTypeId")));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "maxSubClassTypeIdOf", typeIdTypeIdDesc, maxSubClassId);

        StaticIntrinsic isObject = (builder, target, arguments) -> {
            LoadedTypeDefinition jlo = classContext.findDefinedType("java/lang/Object").load();
            return builder.isEq(arguments.get(0), lf.literalOfType(jlo.getObjectType()));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isJavaLangObject", typeIdBooleanDesc, isObject);

        StaticIntrinsic isCloneable = (builder, target, arguments) -> {
            LoadedTypeDefinition jlc = classContext.findDefinedType("java/lang/Cloneable").load();
            return builder.isEq(arguments.get(0), lf.literalOfType(jlc.getObjectType()));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isJavaLangCloneable", typeIdBooleanDesc, isCloneable);

        StaticIntrinsic isSerializable = (builder, target, arguments) -> {
            LoadedTypeDefinition jis = classContext.findDefinedType("java/io/Serializable").load();
            return builder.isEq(arguments.get(0), lf.literalOfType(jis.getObjectType()));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isJavaIoSerializable", typeIdBooleanDesc, isSerializable);

        StaticIntrinsic isClass = (builder, target, arguments) -> {
            LoadedTypeDefinition jlo = classContext.findDefinedType("java/lang/Object").load();
            ValueType refArray = coreClasses.getArrayLoadedTypeDefinition("[ref").getObjectType();
            Value isObj = builder.isEq(arguments.get(0), lf.literalOfType(jlo.getObjectType()));
            Value isAboveRef = builder.isLt(lf.literalOfType(refArray), arguments.get(0));
            Value isNotInterface = builder.isLt(arguments.get(0), lf.literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), tables.getFirstInterfaceTypeId()));
            return builder.or(isObj, builder.and(isAboveRef, isNotInterface));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isClass", typeIdBooleanDesc, isClass);

        StaticIntrinsic isInterface = (builder, target, arguments) ->
            builder.isLe(lf.literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), tables.getFirstInterfaceTypeId()), arguments.get(0));

        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isInterface", typeIdBooleanDesc, isInterface);

        StaticIntrinsic isPrimArray = (builder, target, arguments) -> {
            ValueType firstPrimArray = coreClasses.getArrayLoadedTypeDefinition("[Z").getObjectType();
            ValueType lastPrimArray = coreClasses.getArrayLoadedTypeDefinition("[D").getObjectType();
            return builder.and(builder.isGe(arguments.get(0), lf.literalOfType(firstPrimArray)),
                builder.isLe(arguments.get(0), lf.literalOfType(lastPrimArray)));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isPrimArray", typeIdBooleanDesc, isPrimArray);

        StaticIntrinsic isPrimitive = (builder, target, arguments) -> {
            ValueType firstPrimType = Primitive.VOID.getType();
            ValueType lastPrimType = Primitive.DOUBLE.getType();
            return builder.and(builder.isGe(arguments.get(0), lf.literalOfType(firstPrimType)),
                               builder.isLe(arguments.get(0), lf.literalOfType(lastPrimType)));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "isPrimitive", typeIdBooleanDesc, isPrimitive);

        StaticIntrinsic isRefArray = (builder, target, arguments) -> {
            ValueType refArray = coreClasses.getArrayLoadedTypeDefinition("[ref").getObjectType();
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
            Value typeIdStruct = builder.elementOf(builder.getLiteralFactory().literalOf(typeIdGlobal), objTypeId);
            Value bits = builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("interfaceBits"));
            Value adjustedInterfaceTypeId = builder.sub(interfaceTypeId, lf.literalOf(typeIdLiteralType, tables.getFirstInterfaceTypeId()));
            Value implementsIdx = builder.shr(builder.bitCast(adjustedInterfaceTypeId, typeIdLiteralType.asUnsigned()), lf.literalOf(typeIdLiteralType, 3));
            Value implementsBit = builder.and(adjustedInterfaceTypeId, lf.literalOf(typeIdLiteralType, 7));
            Value dataByte = builder.load(builder.elementOf(bits, builder.extend(implementsIdx, ctxt.getTypeSystem().getSignedInteger32Type())));
            Value mask = builder.truncate(builder.shl(lf.literalOf(typeIdLiteralType, 1), implementsBit), ctxt.getTypeSystem().getSignedInteger8Type());
            return builder.isEq(mask, builder.and(mask, dataByte));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "doesImplement", typeIdTypeIdBooleanDesc, doesImplement);

        StaticIntrinsic getDimFromClass = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), coreClasses.getClassDimensionField()));
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getDimensionsFromClass", clsUint8, getDimFromClass);

        StaticIntrinsic getTypeIdFromClass = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), coreClasses.getClassTypeIdField()));
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getTypeIdFromClass", clsTypeId, getTypeIdFromClass);

        MethodElement getOrCreateArrayClass = methodFinder.getMethod("getOrCreateClassForRefArray");
        ReferenceType jlcRef = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load().getObjectType().getReference();
        StaticIntrinsic getClassFromTypeId = (builder, target, arguments) -> {
            /* Pseudo code for this intrinsic:
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
            ProgramObject rootArray = buildtimeHeap.getAndRegisterGlobalClassArray(builder.getCurrentElement());
            Literal base = lf.literalOf(ProgramObjectPointer.of(rootArray));
            Value elem = builder.elementOf(base, arguments.get(0));
            Value componentClass = builder.valueConvert(elem, jlcRef);
            Value result = componentClass;

            builder.if_(builder.isGt(dims, ctxt.getLiteralFactory().literalOf(0)), trueBranch, fallThrough, Map.of(Slot.temp(0), result)); // if (dimensions > 0)

            builder.begin(trueBranch); // true; create Class for array reference
            result = builder.getFirstBuilder().call(lf.literalOf(getOrCreateArrayClass), List.of(componentClass, dims));
            builder.goto_(fallThrough, Slot.temp(0), result);
            builder.begin(fallThrough);
            return builder.addParam(fallThrough, Slot.temp(0), result.getType());
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getClassFromTypeId", typeIdClsDesc, getClassFromTypeId);

        StaticIntrinsic getClassFromTypeIdSimple = (builder, target, arguments) -> {
            BuildtimeHeap buildtimeHeap = BuildtimeHeap.get(ctxt);
            ProgramObject rootArray = buildtimeHeap.getAndRegisterGlobalClassArray(builder.getCurrentElement());
            Literal base = lf.literalOf(ProgramObjectPointer.of(rootArray));
            Value elem = builder.elementOf(base, arguments.get(0));
            return builder.valueConvert(elem, jlcRef);
        };

        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getClassFromTypeIdSimple", typeIdToClassDesc, getClassFromTypeIdSimple);

        StaticIntrinsic getArrayClassOf = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), CoreClasses.get(ctxt).getArrayClassField()));
        intrinsics.registerIntrinsic(ciDesc, "getArrayClassOf", clsClsDesc, getArrayClassOf);

        StaticIntrinsic setArrayClass = (builder, target, arguments) -> {
            LoadedTypeDefinition jlc = classContext.findDefinedType("java/lang/Class").load();
            ReferenceType refType = jlc.getObjectType().getReference();
            Value expect = ctxt.getLiteralFactory().nullLiteralOfType(refType);
            Value update = arguments.get(1);
            Value result = builder.cmpAndSwap(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), CoreClasses.get(ctxt).getArrayClassField()), expect, update, GlobalAcquire, GlobalRelease, CmpAndSwap.Strength.STRONG);
            // extract the flag
            return builder.extractMember(result, CmpAndSwap.getResultType(ctxt, refType).getMember(1));
        };
        intrinsics.registerIntrinsic(ciDesc, "setArrayClass", clsClsBooleanDesc, setArrayClass);

        InstanceFieldElement jlcName = classContext.findDefinedType("java/lang/Class").load().findInstanceField("name");
        InstanceFieldElement jlcCompType = classContext.findDefinedType("java/lang/Class").load().findInstanceField("componentType");

        StaticIntrinsic createClass = (builder, target, arguments) -> {
            ClassObjectType jlcType = (ClassObjectType) ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load().getObjectType();
            CompoundType compoundType = Layout.get(ctxt).getInstanceLayoutInfo(jlcType.getDefinition()).getCompoundType();
            Value instance = builder.new_(jlcType, lf.literalOfType(jlcType), lf.literalOf(compoundType.getSize()), lf.literalOf(compoundType.getAlign()));
            Value handle = builder.instanceFieldOf(builder.decodeReference(instance), jlcName);
            builder.store(handle, arguments.get(0), handle.getDetectedMode().getWriteAccess());
            handle = builder.instanceFieldOf(builder.decodeReference(instance), CoreClasses.get(ctxt).getClassTypeIdField());
            builder.store(handle, arguments.get(1), handle.getDetectedMode().getWriteAccess());
            handle = builder.instanceFieldOf(builder.decodeReference(instance), CoreClasses.get(ctxt).getClassDimensionField());
            builder.store(handle, arguments.get(2), handle.getDetectedMode().getWriteAccess());
            handle = builder.instanceFieldOf(builder.decodeReference(instance), jlcCompType);
            builder.store(handle, arguments.get(3), handle.getDetectedMode().getWriteAccess());
            return instance;
        };
        intrinsics.registerIntrinsic(Phase.ADD, ciDesc, "createClass", createClassDesc, createClass);

        StaticIntrinsic getNumberOfTypeIds = (builder, target, arguments) -> lf.literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), tables.get_number_of_typeids());
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getNumberOfTypeIds", emptyTotypeIdDesc, getNumberOfTypeIds);

        StaticIntrinsic callRuntimeInitializer = (builder, target, arguments) -> {
            Value index = arguments.get(0);
            GlobalVariableElement rtinitTable = DispatchTables.get(ctxt).getRTInitsGlobal();
            ProgramModule programModule = ctxt.getOrAddProgramModule(builder.getCurrentElement().getEnclosingType());
            programModule.declareData(null, rtinitTable.getName(), rtinitTable.getType());
            Value initFunc = builder.load(builder.elementOf(builder.getLiteralFactory().literalOf(rtinitTable), index));
            final BlockParameter thrPtr = builder.getParam(builder.getEntryLabel(), Slot.thread());
            return builder.call(initFunc, List.of(thrPtr));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "callRuntimeInitializer", intVoidDesc, callRuntimeInitializer);

        // public static native CNative.type_id getSuperClassTypeId(CNative.type_id typeId);
        StaticIntrinsic getSuperClassTypeId = (builder, target, arguments) -> {
            Value typeId = arguments.get(0);
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            Value typeIdStruct = builder.elementOf(builder.getLiteralFactory().literalOf(typeIdGlobal), typeId);
            Value superTypeId = builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("superTypeId"));
            return builder.load(superTypeId);
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getSuperClassTypeId", typeIdTypeIdDesc, getSuperClassTypeId);

        // public static native CNative.type_id getFirstInterfaceTypeId();
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getFirstInterfaceTypeid", emptyTotypeIdDesc,
            (builder, target, arguments) -> lf.literalOf(ctxt.getTypeSystem().getTypeIdLiteralType(), tables.getFirstInterfaceTypeId()));

        // public static native int getNumberOfBytesInInterfaceBitsArray();
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getNumberOfBytesInInterfaceBitsArray", IntDesc,
            (builder, target, arguments) -> lf.literalOf(tables.getNumberOfBytesInInterfaceBitsArray()));

        // public static native byte getByteOfInterfaceBits(CNative.type_id typeId, int index);
        StaticIntrinsic getByteOfInterfaceBits = (builder, target, arguments) -> {
            Value typeId = arguments.get(0);
            Value index = arguments.get(1);
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            Value typeIdStruct = builder.elementOf(builder.getLiteralFactory().literalOf(typeIdGlobal), typeId);
            Value bits = builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("interfaceBits"));
            return builder.load(builder.elementOf(bits, index));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "getByteOfInterfaceBits", typeIdIntToByteDesc, getByteOfInterfaceBits);
    }

    static void registerOrgQbiccRuntimeBuildIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor buildDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/Build");

        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        BooleanLiteral falseLit = classContext.getLiteralFactory().literalOf(false);
        BooleanLiteral trueLit = classContext.getLiteralFactory().literalOf(true);

        StaticIntrinsic isHost = (builder, target, arguments) -> falseLit;
        StaticIntrinsic isTarget = (builder, target, arguments) -> trueLit;

        intrinsics.registerIntrinsic(Phase.ANALYZE, buildDesc, "isHost", emptyToBool, isHost);
        intrinsics.registerIntrinsic(Phase.ANALYZE, buildDesc, "isTarget", emptyToBool, isTarget);
    }

    static void registerOrgQbiccRuntimeMainIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor mainDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/org/qbicc/runtime/Main");

        ClassTypeDescriptor tgDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ThreadGroup");
        MethodDescriptor voidVoidDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());

        // Get system thread group
        StaticIntrinsic sysThrGrp = (builder, target, arguments) -> ctxt.getLiteralFactory().literalOf(ctxt.getVm().getMainThreadGroup());
        MethodDescriptor returnTgDesc = MethodDescriptor.synthesize(classContext, tgDesc, List.of());
        intrinsics.registerIntrinsic(mainDesc, "getSystemThreadGroup", returnTgDesc, sysThrGrp);
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

    private static void registerJavaLangIntegerIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor integerDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Integer");

        MethodDescriptor intToInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.I));
        MethodDescriptor intIntToInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.I, BaseTypeDescriptor.I));
        MethodDescriptor intToLong = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.I));

        StaticIntrinsic compare = (builder, targetPtr, arguments) -> builder.cmp(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(integerDesc, "compare", intIntToInt, compare);

        StaticIntrinsic compareUnsigned = (builder, targetPtr, arguments) -> {
            final TypeSystem ts = builder.getTypeSystem();
            final Value a = arguments.get(0);
            final Value b = arguments.get(1);
            final UnsignedIntegerType u32 = ts.getUnsignedInteger32Type();
            return builder.cmp(
                builder.bitCast(a, u32),
                builder.bitCast(b, u32)
            );
        };

        intrinsics.registerIntrinsic(integerDesc, "compareUnsigned", intIntToInt, compareUnsigned);

        StaticIntrinsic divideUnsigned = (builder, targetPtr, arguments) -> {
            final TypeSystem ts = builder.getTypeSystem();
            final Value a = arguments.get(0);
            final Value b = arguments.get(1);
            final UnsignedIntegerType u32 = ts.getUnsignedInteger32Type();
            final SignedIntegerType s32 = ts.getSignedInteger32Type();
            return builder.bitCast(builder.divide(
                builder.bitCast(a, u32),
                builder.divisorCheck(builder.bitCast(b, u32))
            ), s32);
        };

        intrinsics.registerIntrinsic(integerDesc, "divideUnsigned", intIntToInt, divideUnsigned);

        StaticIntrinsic remainderUnsigned = (builder, targetPtr, arguments) -> {
            final TypeSystem ts = builder.getTypeSystem();
            final Value a = arguments.get(0);
            final Value b = arguments.get(1);
            final UnsignedIntegerType u32 = ts.getUnsignedInteger32Type();
            final SignedIntegerType s32 = ts.getSignedInteger32Type();
            return builder.bitCast(builder.remainder(
                builder.bitCast(a, u32),
                builder.divisorCheck(builder.bitCast(b, u32))
            ), s32);
        };

        intrinsics.registerIntrinsic(integerDesc, "remainderUnsigned", intIntToInt, remainderUnsigned);

        StaticIntrinsic toUnsignedLong = (builder, targetPtr, arguments) -> {
            final TypeSystem ts = builder.getTypeSystem();
            final UnsignedIntegerType u32 = ts.getUnsignedInteger32Type();
            final UnsignedIntegerType u64 = ts.getUnsignedInteger64Type();
            final SignedIntegerType s64 = ts.getSignedInteger64Type();
            return builder.bitCast(builder.extend(builder.bitCast(arguments.get(0), u32), u64), s64);
        };

        intrinsics.registerIntrinsic(integerDesc, "toUnsignedLong", intToLong, toUnsignedLong);

        StaticIntrinsic numberOfTrailingZeros = (builder, targetPtr, arguments) -> builder.countTrailingZeros(arguments.get(0));

        intrinsics.registerIntrinsic(integerDesc, "numberOfTrailingZeros", intToInt, numberOfTrailingZeros);

        StaticIntrinsic numberOfLeadingZeros = (builder, targetPtr, arguments) -> builder.countLeadingZeros(arguments.get(0));

        intrinsics.registerIntrinsic(integerDesc, "numberOfLeadingZeros", intToInt, numberOfLeadingZeros);

        StaticIntrinsic highestOneBit = (builder, targetPtr, arguments) -> {
            // todo: builder.highestOneBit(arguments.get(0));
            // for now: a0 & (MIN_VALUE >>> numberOfLeadingZeros(a0));
            final LiteralFactory lf = builder.getLiteralFactory();
            final TypeSystem ts = builder.getTypeSystem();
            final UnsignedIntegerType u32 = ts.getUnsignedInteger32Type();
            final SignedIntegerType s32 = ts.getSignedInteger32Type();
            final IntegerLiteral bit = lf.literalOf(u32, Integer.MIN_VALUE);
            return builder.and(arguments.get(0), builder.bitCast(builder.shr(bit, builder.bitCast(builder.countLeadingZeros(arguments.get(0)), u32)), s32));
        };

        intrinsics.registerIntrinsic(integerDesc, "highestOneBit", intToInt, highestOneBit);

        StaticIntrinsic lowestOneBit = (builder, targetPtr, arguments) -> {
            // todo: builder.lowestOneBit(arguments.get(0))
            // for now: a0 & -a0
            return builder.and(arguments.get(0), builder.negate(arguments.get(0)));
        };

        intrinsics.registerIntrinsic(integerDesc, "lowestOneBit", intToInt, lowestOneBit);

        StaticIntrinsic bitCount = ((builder, targetPtr, arguments) -> builder.populationCount(arguments.get(0)));

        intrinsics.registerIntrinsic(integerDesc, "bitCount", intToInt, bitCount);
    }

    private static void registerJavaLangLongIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor longDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Long");

        MethodDescriptor longLongToInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.J, BaseTypeDescriptor.J));
        MethodDescriptor longLongToLong = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.J, BaseTypeDescriptor.J));

        StaticIntrinsic compare = (builder, targetPtr, arguments) -> builder.cmp(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(longDesc, "compare", longLongToInt, compare);

        StaticIntrinsic compareUnsigned = (builder, targetPtr, arguments) -> {
            final TypeSystem ts = builder.getTypeSystem();
            final Value a = arguments.get(0);
            final Value b = arguments.get(1);
            final UnsignedIntegerType u64 = ts.getUnsignedInteger64Type();
            return builder.cmp(
                builder.bitCast(a, u64),
                builder.bitCast(b, u64)
            );
        };

        intrinsics.registerIntrinsic(longDesc, "compareUnsigned", longLongToInt, compareUnsigned);

        StaticIntrinsic divideUnsigned = (builder, targetPtr, arguments) -> {
            final TypeSystem ts = builder.getTypeSystem();
            final Value a = arguments.get(0);
            final Value b = arguments.get(1);
            final UnsignedIntegerType u64 = ts.getUnsignedInteger64Type();
            final SignedIntegerType s64 = ts.getSignedInteger64Type();
            return builder.bitCast(builder.divide(
                builder.bitCast(a, u64),
                builder.divisorCheck(builder.bitCast(b, u64))
            ), s64);
        };

        intrinsics.registerIntrinsic(longDesc, "divideUnsigned", longLongToLong, divideUnsigned);

        StaticIntrinsic remainderUnsigned = (builder, targetPtr, arguments) -> {
            final TypeSystem ts = builder.getTypeSystem();
            final Value a = arguments.get(0);
            final Value b = arguments.get(1);
            final UnsignedIntegerType u64 = ts.getUnsignedInteger64Type();
            final SignedIntegerType s64 = ts.getSignedInteger64Type();
            return builder.bitCast(builder.remainder(
                builder.bitCast(a, u64),
                builder.divisorCheck(builder.bitCast(b, u64))
            ), s64);
        };

        intrinsics.registerIntrinsic(longDesc, "remainderUnsigned", longLongToLong, remainderUnsigned);
    }

    private static void registerJavaLangRefIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor referenceDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ref/Reference");
        ClassTypeDescriptor phantomReferenceDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ref/PhantomReference");

        MethodDescriptor objToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(objDesc));
        MethodDescriptor objToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(objDesc));

        InstanceIntrinsic refersTo0 = (builder, instance, target, arguments) ->
            builder.isEq(
                arguments.get(0),
                builder.load(builder.instanceFieldOf(builder.decodeReference(instance), referenceDesc, "referent", objDesc))
            );

        intrinsics.registerIntrinsic(Phase.ADD, referenceDesc, "refersTo0", objToBool, refersTo0);
        intrinsics.registerIntrinsic(Phase.ADD, phantomReferenceDesc, "refersTo0", objToBool, refersTo0);

        StaticIntrinsic reachabilityFence = (builder, target, arguments) -> {
            builder.reachable(arguments.get(0));
            final ClassContext context = builder.getCurrentElement().getEnclosingType().getContext();
            final TypeSystem ts = context.getTypeSystem();
            final LiteralFactory lf = context.getLiteralFactory();
            return lf.zeroInitializerLiteralOfType(ts.getVoidType());
        };

        intrinsics.registerIntrinsic(Phase.ADD, referenceDesc, "reachabilityFence", objToVoid, reachabilityFence);
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

        ClassTypeDescriptor jlc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor jls = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor jlo = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor classloader = ClassTypeDescriptor.synthesize(classContext, "java/lang/ClassLoader");
        ClassTypeDescriptor reflect = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/reflect/Reflection");

        MethodDescriptor boolStringObj = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(jls, jlo));
        MethodDescriptor intClass = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(jlc));

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
            InstanceFieldElement field = ltd.findInstanceField(fieldName);
            if (field == null) {
                ctxt.error(builder.getLocation(), "No such field \"%s\" on class \"%s\"", fieldName, ltd.getVmClass().getName());
                return lf.literalOf(false);
            }

            ValueType expectType = newValue.getType();
            Value result = builder.cmpAndSwap(builder.instanceFieldOf(builder.decodeReference(input), field), lf.zeroInitializerLiteralOfType(expectType),
                newValue, GlobalSeqCst, SingleOpaque, CmpAndSwap.Strength.STRONG);
            // result is a compound structure; extract the success flag
            return builder.extractMember(result, CmpAndSwap.getResultType(ctxt, expectType).getMember(1));
        };

        intrinsics.registerIntrinsic(classloader, "trySetObjectField", boolStringObj, trySetObjectField);

        // Implement as an intrinsic as the simplest way to cross package boundaries without exposing anything sensitive
        InstanceFieldElement modField = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load().findInstanceField("modifiers");
        StaticIntrinsic getClassAccessFlags = (builder, input, arguments) -> {
            Value cls = arguments.get(0);
            // return cls.modifiers & 0x1ff
            return builder.and(builder.load(builder.instanceFieldOf(builder.decodeReference(cls), modField)), ctxt.getLiteralFactory().literalOf(0x1fff));
        };
        intrinsics.registerIntrinsic(Phase.ANALYZE, reflect, "getClassAccessFlags", intClass, getClassAccessFlags);
    }
}

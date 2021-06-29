package org.qbicc.plugin.intrinsics.core;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Driver;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.Extend;
import org.qbicc.graph.Load;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.Variable;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.machine.probe.CProbe;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayTables;
import org.qbicc.plugin.intrinsics.InstanceIntrinsic;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.plugin.intrinsics.StaticIntrinsic;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.CompoundType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.WordType;
import org.qbicc.type.CompoundType.Member;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
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
    public static void register(CompilationContext ctxt) {
        registerJavaLangClassIntrinsics(ctxt);
        registerJavaLangStringUTF16Intrinsics(ctxt);
        registerJavaLangClassLoaderIntrinsics(ctxt);
        registerJavaLangSystemIntrinsics(ctxt);
        registerJavaLangThreadIntrinsics(ctxt);
        registerJavaLangThrowableIntrinsics(ctxt);
        registerJavaLangObjectIntrinsics(ctxt);
        registerJavaLangNumberIntrinsics(ctxt);
        registerJavaLangFloatDoubleMathIntrinsics(ctxt);
        registerOrgQbiccRuntimeCNativeIntrinsics(ctxt);
        registerOrgQbiccObjectModelIntrinsics(ctxt);
        registerOrgQbiccRuntimeMainIntrinsics(ctxt);
        registerOrgQbiccRuntimeValuesIntrinsics(ctxt);
        registerJavaLangMathIntrinsics(ctxt);
        registerOrgQbiccRuntimePosixPthreadCastPtr(ctxt);
    }

    private static StaticIntrinsic setVolatile(CompilationContext ctxt, FieldElement field) {
        Literal voidLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());
        return (builder, target, arguments) -> {
            builder.store(builder.staticField(field), arguments.get(0), MemoryAtomicityMode.VOLATILE);
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
        MethodDescriptor emptyToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());
        MethodDescriptor emptyToString = MethodDescriptor.synthesize(classContext, jlsDesc, List.of());
        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());
        MethodDescriptor stringToClass = MethodDescriptor.synthesize(classContext, jlcDesc, List.of(jlsDesc));
        MethodDescriptor objToObj = MethodDescriptor.synthesize(classContext, jloDesc, List.of(jloDesc));

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
            builder.getFirstBuilder().call(builder.staticMethod(helper), List.of(arguments.get(0), instance));

            // Generics erasure issue. The return type of Class<T>.cast is T, but it gets wiped to Object.
            // If the result of this cast is actually used as a T, there will be a (redundant) checkcast bytecode following this operation.
            ReferenceType jlot = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load().getType().getReference();
            return builder.bitCast(arguments.get(0), jlot);
        };

        Literal voidLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());

        StaticIntrinsic registerNatives = (builder, target, arguments) -> voidLiteral;

        InstanceIntrinsic initClassName = (builder, instance, target, arguments) -> {
            // not reachable; we always would initialize our class name eagerly
            throw new BlockEarlyTermination(builder.unreachable());
        };

        StaticIntrinsic getPrimitiveClass = (builder, target, arguments) -> {
            // always called with a string literal
            StringLiteral lit = (StringLiteral) arguments.get(0);
            LiteralFactory lf = ctxt.getLiteralFactory();
            TypeSystem ts = ctxt.getTypeSystem();
            ValueType type;
            switch (lit.getValue()) {
                case "byte": type = ts.getSignedInteger8Type(); break;
                case "short": type = ts.getSignedInteger16Type(); break;
                case "int": type = ts.getSignedInteger32Type(); break;
                case "long": type = ts.getSignedInteger64Type(); break;

                case "char": type = ts.getUnsignedInteger16Type(); break;

                case "float": type = ts.getFloat32Type(); break;
                case "double": type = ts.getFloat64Type(); break;

                case "boolean": type = ts.getBooleanType(); break;

                case "void": type = ts.getVoidType(); break;

                default: {
                    ctxt.error(builder.getLocation(), "Invalid argument to `getPrimitiveClass`: %s", lit.getValue());
                    throw new BlockEarlyTermination(builder.unreachable());
                }
            }
            return builder.classOf(lf.literalOfType(type));
        };

        //    static native Class<?> getPrimitiveClass(String name);

        intrinsics.registerIntrinsic(jlcDesc, "cast", objToObj, cast);
        intrinsics.registerIntrinsic(jlcDesc, "desiredAssertionStatus0", classToBool, desiredAssertionStatus0);
        intrinsics.registerIntrinsic(jlcDesc, "desiredAssertionStatus", emptyToBool, desiredAssertionStatus);
        intrinsics.registerIntrinsic(jlcDesc, "registerNatives", emptyToVoid, registerNatives);
        intrinsics.registerIntrinsic(jlcDesc, "initClassName", emptyToString, initClassName);
        intrinsics.registerIntrinsic(jlcDesc, "getPrimitiveClass", stringToClass, getPrimitiveClass);
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

    public static void registerJavaLangClassLoaderIntrinsics(CompilationContext ctxt) {

        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jlclDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ClassLoader");

        MethodDescriptor emptyToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());
        Literal voidLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());

        StaticIntrinsic registerNatives = (builder, target, arguments) -> voidLiteral;

        intrinsics.registerIntrinsic(jlclDesc, "registerNatives", emptyToVoid, registerNatives);
    }

    public static void registerJavaLangSystemIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor systemDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/System");
        LoadedTypeDefinition jls = classContext.findDefinedType("java/lang/System").load();
        ClassTypeDescriptor jloDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
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

        MethodDescriptor setPrintStreamDesc =
            MethodDescriptor.synthesize(classContext,
                BaseTypeDescriptor.V, List.of(ClassTypeDescriptor.synthesize(classContext, "java/io/PrintStream")));

        intrinsics.registerIntrinsic(systemDesc, "setIn", setPrintStreamDesc, setVolatile(ctxt, in));
        intrinsics.registerIntrinsic(systemDesc, "setOut", setPrintStreamDesc, setVolatile(ctxt, out));
        intrinsics.registerIntrinsic(systemDesc, "setErr", setPrintStreamDesc, setVolatile(ctxt, err));

        // arraycopy

        MethodDescriptor arraycopyDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(
            jloDesc,
            BaseTypeDescriptor.I,
            jloDesc,
            BaseTypeDescriptor.I,
            BaseTypeDescriptor.I
        ));

        StaticIntrinsic arraycopy = (builder, target, arguments) ->
            builder.call(builder.staticMethod(vmDesc, "arraycopy", target.getDescriptor()), arguments);

        intrinsics.registerIntrinsic(systemDesc, "arraycopy", arraycopyDesc, arraycopy);

        // identity hash code

        // todo: obviously non-optimal; replace once we have object headers sorted out
        StaticIntrinsic identityHashCode = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(0);

        intrinsics.registerIntrinsic(systemDesc, "identityHashCode", objectToIntDesc, identityHashCode);
    }

    public static void registerJavaLangThreadIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jltDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Thread");
        MethodDescriptor returnJlt = MethodDescriptor.synthesize(classContext, jltDesc, List.of());

        StaticIntrinsic currentThread = (builder, target, arguments) -> builder.currentThread();

        intrinsics.registerIntrinsic(jltDesc, "currentThread", returnJlt, currentThread);

        StaticIntrinsic nop = (builder, target, arguments) -> ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getType().getReturnType());

        intrinsics.registerIntrinsic(jltDesc, "registerNatives", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of()), nop);
    }

    public static void registerJavaLangThrowableIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jltDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Throwable");
        ClassTypeDescriptor steDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/StackTraceElement");
        ArrayTypeDescriptor steArrayDesc = ArrayTypeDescriptor.of(classContext, steDesc);

        Literal zero = ctxt.getLiteralFactory().literalOf(ctxt.getTypeSystem().getSignedInteger32Type(), 0);

        // todo: temporary, until we have a stack walker

        InstanceIntrinsic fillInStackTrace = (builder, instance, target, arguments) ->
            instance;

        InstanceIntrinsic getStackTrace = (builder, instance, target, arguments) ->
            builder.newArray(steArrayDesc, zero);

        intrinsics.registerIntrinsic(jltDesc, "fillInStackTrace", MethodDescriptor.synthesize(classContext, jltDesc, List.of()), fillInStackTrace);
        intrinsics.registerIntrinsic(jltDesc, "getStackTrace", MethodDescriptor.synthesize(classContext, steArrayDesc, List.of()), getStackTrace);
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

        StaticIntrinsic ror = (builder, target, arguments) ->
            builder.ror(arguments.get(0), arguments.get(1));

        StaticIntrinsic rol = (builder, target, arguments) ->
            builder.rol(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(integerDesc, "rotateRight", binaryIntDesc, ror);
        intrinsics.registerIntrinsic(longDesc, "rotateRight", longIntDesc, ror);

        intrinsics.registerIntrinsic(integerDesc, "rotateLeft", binaryIntDesc, rol);
        intrinsics.registerIntrinsic(longDesc, "rotateLeft", longIntDesc, rol);

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

        // Object#getClass()Ljava/lang/Class; --> field read of the "typeId" field
        MethodDescriptor getClassDesc =
            MethodDescriptor.synthesize(classContext,
                ClassTypeDescriptor.synthesize(classContext, "java/lang/Class"), List.of());
        InstanceIntrinsic getClassIntrinsic = (builder, instance, target, arguments) ->
            builder.classOf(builder.typeIdOf(builder.referenceHandle(instance)));
        intrinsics.registerIntrinsic(objDesc, "getClass", getClassDesc, getClassIntrinsic);

        // Object#hashCode TODO redo when object headers are set
        MethodDescriptor hashCodeDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of());
        InstanceIntrinsic hashCodeIntrinsic = (builder, instance, target, arguments)->
            ctxt.getLiteralFactory().literalOf(0);
        intrinsics.registerIntrinsic(objDesc, "hashCode", hashCodeDesc, hashCodeIntrinsic);

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

        PointerType voidPtr = ctxt.getTypeSystem().getVoidType().getPointer();

        StaticIntrinsic typeOf = (builder, target, arguments) ->
            builder.typeIdOf(builder.referenceHandle(arguments.get(0)));

        intrinsics.registerIntrinsic(cNativeDesc, "type_id_of", objTypeIdDesc, typeOf);

        FieldElement elementTypeField = Layout.get(ctxt).getRefArrayElementTypeIdField();

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
            ValueHandle threadRef = builder.referenceHandle(thread);
            builder.store(builder.instanceFieldOf(threadRef, nameFld), arguments.get(0), MemoryAtomicityMode.NONE);
            builder.store(builder.instanceFieldOf(threadRef, groupFld), arguments.get(1), MemoryAtomicityMode.NONE);
            Value tid = builder.call(builder.staticMethod(thrDesc, "nextThreadID", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of())), List.of());
            builder.store(builder.instanceFieldOf(threadRef, tidFld), tid, MemoryAtomicityMode.NONE);
            // set thread to be running with JVMTI status for RUNNABLE and ALIVE
            builder.store(builder.instanceFieldOf(threadRef, threadStatusFld), ctxt.getLiteralFactory().literalOf(0x05), MemoryAtomicityMode.NONE);
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());
        };

        intrinsics.registerIntrinsic(cNativeDesc, "attachNewThread", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(
            strDesc, tgDesc
        )), attachNewThread);

        InstanceIntrinsic xxxValue = (builder, instance, target, arguments) -> {
            WordType to = (WordType) target.getType().getReturnType();
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

        InstanceIntrinsic deref = (builder, instance, target, arguments) -> builder.load(builder.pointerHandle(instance), MemoryAtomicityMode.NONE);

        intrinsics.registerIntrinsic(ptrDesc, "deref", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), deref);

        InstanceIntrinsic identity = (builder, instance, target, arguments) -> instance;

        intrinsics.registerIntrinsic(ptrDesc, "asArray", MethodDescriptor.synthesize(classContext, ArrayTypeDescriptor.of(classContext, nObjDesc), List.of()), identity);

        InstanceIntrinsic get = (builder, instance, target, arguments) ->
            builder.load(builder.elementOf(builder.pointerHandle(instance), arguments.get(0)), MemoryAtomicityMode.NONE);

        intrinsics.registerIntrinsic(ptrDesc, "get", MethodDescriptor.synthesize(classContext, nObjDesc, List.of(BaseTypeDescriptor.I)), get);

        InstanceIntrinsic set = (builder, instance, target, arguments) -> {
            builder.store(builder.elementOf(builder.pointerHandle(instance), arguments.get(0)), arguments.get(1), MemoryAtomicityMode.NONE);
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getType().getReturnType());
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
            ValueType argType = arguments.get(0).getType();
            long size;
            if (argType instanceof TypeType) {
                size = ((TypeType) argType).getUpperBound().getSize();
            } else {
                size = argType.getSize();
            }
            IntegerType returnType = (IntegerType) target.getType().getReturnType();
            return ctxt.getLiteralFactory().literalOf(returnType, size);
        };

        intrinsics.registerIntrinsic(cNativeDesc, "sizeof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(nObjDesc)), sizeof);
        intrinsics.registerIntrinsic(cNativeDesc, "sizeof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(classDesc)), sizeof);
        intrinsics.registerIntrinsic(cNativeDesc, "sizeof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(ArrayTypeDescriptor.of(classContext, nObjDesc))), sizeof);
        intrinsics.registerIntrinsic(cNativeDesc, "sizeofArray", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(ArrayTypeDescriptor.of(classContext, classDesc))), sizeof);

        StaticIntrinsic alignof = (builder, target, arguments) -> {
            ValueType argType = arguments.get(0).getType();
            long align;
            if (argType instanceof TypeType) {
                align = ((TypeType) argType).getUpperBound().getAlign();
            } else {
                align = argType.getAlign();
            }
            IntegerType returnType = (IntegerType) target.getType().getReturnType();
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

        StaticIntrinsic constant = (builder, target, arguments) ->
            ctxt.getLiteralFactory().constantLiteralOfType(ctxt.getTypeSystem().getPoisonType());

        intrinsics.registerIntrinsic(cNativeDesc, "constant", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), constant);

        StaticIntrinsic bitCast = (builder, target, arguments) ->
            builder.bitCast(arguments.get(0), (WordType) target.getType().getReturnType());

        intrinsics.registerIntrinsic(cNativeDesc, "ptrToRef", MethodDescriptor.synthesize(classContext, objDesc, List.of(ptrDesc)), bitCast);
        intrinsics.registerIntrinsic(cNativeDesc, "refToPtr", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(objDesc)), bitCast);
    }

    static void registerOrgQbiccObjectModelIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        Layout layout = Layout.get(ctxt);
        SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
        LiteralFactory lf = ctxt.getLiteralFactory();

        ClassTypeDescriptor objModDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/ObjectModel");
        ClassTypeDescriptor typeIdDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$type_id");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor clsDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor uint8Desc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$uint8_t");

        MethodDescriptor objTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(objDesc));
        MethodDescriptor objUint8Desc = MethodDescriptor.synthesize(classContext, uint8Desc, List.of(objDesc));
        MethodDescriptor typeIdTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(typeIdDesc));
        MethodDescriptor typeIdBooleanDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(typeIdDesc));
        MethodDescriptor typeIdTypeIdBooleanDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(typeIdDesc, typeIdDesc));
        MethodDescriptor typeIdVoidDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(typeIdDesc));
        MethodDescriptor typeIdIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(typeIdDesc));
        MethodDescriptor clsTypeId = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(clsDesc));
        MethodDescriptor clsUint8 = MethodDescriptor.synthesize(classContext, uint8Desc, List.of(clsDesc));
        MethodDescriptor IntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of());
        MethodDescriptor emptyTotypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of());
        MethodDescriptor typeIdIntToByteDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.B, List.of(typeIdDesc, BaseTypeDescriptor.I));

        StaticIntrinsic typeOf = (builder, target, arguments) ->
            builder.typeIdOf(builder.referenceHandle(arguments.get(0)));
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "type_id_of", objTypeIdDesc, typeOf);

        FieldElement elementTypeField = layout.getRefArrayElementTypeIdField();
        StaticIntrinsic elementTypeOf = (builder, target, arguments) -> {
            ValueHandle handle = builder.referenceHandle(builder.bitCast(arguments.get(0), elementTypeField.getEnclosingType().load().getType().getReference()));
            return builder.load(builder.instanceFieldOf(handle, elementTypeField), MemoryAtomicityMode.UNORDERED);
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "element_type_id_of", objTypeIdDesc, elementTypeOf);

        FieldElement dimensionsField = layout.getRefArrayDimensionsField();
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
            ValueType refArray = layout.getArrayLoadedTypeDefinition("[ref").getType();
            Value isObj = builder.isEq(arguments.get(0), lf.literalOfType(jlo.getType()));
            Value isAboveRef = builder.isLt(lf.literalOfType(refArray), arguments.get(0));
            Value isNotInterface = builder.isLt(arguments.get(0), lf.literalOf(tables.getFirstInterfaceTypeId()));
            return builder.or(isObj, builder.and(isAboveRef, isNotInterface));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_class", typeIdBooleanDesc, isClass);

        StaticIntrinsic isInterface = (builder, target, arguments) ->
            builder.isLe(lf.literalOf(tables.getFirstInterfaceTypeId()), arguments.get(0));

        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_interface", typeIdBooleanDesc, isInterface);

        StaticIntrinsic isPrimArray = (builder, target, arguments) -> {
            ValueType firstPrimArray = layout.getArrayLoadedTypeDefinition("[Z").getType();
            ValueType lastPrimArray = layout.getArrayLoadedTypeDefinition("[D").getType();
            return builder.and(builder.isLe(lf.literalOfType(firstPrimArray), arguments.get(0)),
                builder.isGe(arguments.get(0), lf.literalOfType(lastPrimArray)));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_prim_array", typeIdBooleanDesc, isPrimArray);

        StaticIntrinsic isRefArray = (builder, target, arguments) -> {
            ValueType refArray = layout.getArrayLoadedTypeDefinition("[ref").getType();
            return builder.isEq(arguments.get(0), lf.literalOfType(refArray));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_reference_array", typeIdBooleanDesc, isRefArray);

        StaticIntrinsic doesImplement = (builder, target, arguments) -> {
            Value objTypeId = arguments.get(0);
            Value interfaceTypeId = arguments.get(1);
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            ValueHandle typeIdStruct = builder.elementOf(builder.globalVariable(typeIdGlobal), objTypeId);
            ValueHandle bits = builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("interfaceBits"));
            Value adjustedInterfaceTypeId = builder.sub(interfaceTypeId, lf.literalOf(tables.getFirstInterfaceTypeId()));
            Value implementsIdx = builder.shr(builder.bitCast(adjustedInterfaceTypeId, ctxt.getTypeSystem().getUnsignedInteger32Type()), lf.literalOf(3));
            Value implementsBit = builder.and(adjustedInterfaceTypeId, lf.literalOf(7));
            Value dataByte = builder.load(builder.elementOf(bits, implementsIdx), MemoryAtomicityMode.UNORDERED);
            Value mask = builder.truncate(builder.shl(lf.literalOf(1), implementsBit), ctxt.getTypeSystem().getSignedInteger8Type());
            return builder.isEq(mask, builder.and(mask, dataByte));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "does_implement", typeIdTypeIdBooleanDesc, doesImplement);

        StaticIntrinsic getDimFromClass = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), layout.getClassDimensionField()), MemoryAtomicityMode.UNORDERED);
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_dimensions_from_class", clsUint8, getDimFromClass);

        StaticIntrinsic getTypeIdFromClass = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), layout.getClassTypeIdField()), MemoryAtomicityMode.UNORDERED);
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_type_id_from_class", clsTypeId, getTypeIdFromClass);

        StaticIntrinsic getNumberOfTypeIds = (builder, target, arguments) -> lf.literalOf(tables.get_number_of_typeids());
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_number_of_typeids", IntDesc, getNumberOfTypeIds);

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
            return lf.literalOf(tables.getFirstInterfaceTypeId());
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

        // todo: compareAndSwap*

        // getAndSet*

        class GetAndSetIntrinsic implements StaticIntrinsic {
            private final MemoryAtomicityMode mode;

            GetAndSetIntrinsic(MemoryAtomicityMode mode) {
                this.mode = mode;
            }

            @Override
            public Value emitIntrinsic(BasicBlockBuilder builder, MethodElement element, List<Value> arguments) {
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
            public Value emitIntrinsic(BasicBlockBuilder builder, MethodElement element, List<Value> arguments) {
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
            public Value emitIntrinsic(BasicBlockBuilder builder, MethodElement element, List<Value> arguments) {
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
}

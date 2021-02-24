package cc.quarkus.qcc.plugin.intrinsics.core;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.Load;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.graph.Variable;
import cc.quarkus.qcc.graph.literal.BooleanLiteral;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.ZeroInitializerLiteral;
import cc.quarkus.qcc.plugin.intrinsics.InstanceValueIntrinsic;
import cc.quarkus.qcc.plugin.intrinsics.Intrinsics;
import cc.quarkus.qcc.plugin.intrinsics.StaticIntrinsic;
import cc.quarkus.qcc.plugin.intrinsics.StaticValueIntrinsic;
import cc.quarkus.qcc.plugin.layout.Layout;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.descriptor.ArrayTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.BaseTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

/**
 * Core JDK intrinsics.
 */
public final class CoreIntrinsics {
    public static void register(CompilationContext ctxt) {
        registerJavaLangClassIntrinsics(ctxt);
        registerJavaLangSystemIntrinsics(ctxt);
        registerJavaLangThreadIntrinsics(ctxt);
        registerJavaLangThrowableIntrinsics(ctxt);
        registerJavaLangObjectIntrinsics(ctxt);
        registerJavaLangNumberIntrinsics(ctxt);
        registerJavaLangFloatDoubleMathIntrinsics(ctxt);
        registerCcQuarkusQccRuntimeValuesIntrinsics(ctxt);
        registerJavaLangMathIntrinsics(ctxt);
    }

    private static StaticIntrinsic setVolatile(FieldElement field) {
        return (builder, owner, name, descriptor, arguments) -> builder.store(builder.staticField(field), arguments.get(0), MemoryAtomicityMode.VOLATILE);
    }

    public static void registerJavaLangClassIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jlcDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");

        MethodDescriptor classToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(jlcDesc));
        MethodDescriptor emptyToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());

        // Assertion status

        // todo: this probably belongs in the class libraries rather than here
        StaticValueIntrinsic desiredAssertionStatus0 = (builder, owner, name, descriptor, arguments) ->
            classContext.getLiteralFactory().literalOf(false);

        StaticIntrinsic registerNatives = (builder, owner, name, descriptor, arguments) -> builder.nop();

        intrinsics.registerIntrinsic(jlcDesc, "desiredAssertionStatus0", classToBool, desiredAssertionStatus0);
        intrinsics.registerIntrinsic(jlcDesc, "registerNatives", emptyToVoid, registerNatives);
    }

    public static void registerJavaLangSystemIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor systemDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/System");
        ValidatedTypeDefinition jls = classContext.findDefinedType("java/lang/System").validate();

        // Null and no-operation intrinsics

        StaticValueIntrinsic returnNull = (builder, owner, name, descriptor, arguments) ->
            classContext.getLiteralFactory().zeroInitializerLiteralOfType(jls.getClassType().getReference().asNullable());
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
                BaseTypeDescriptor.V, List.of(ClassTypeDescriptor.synthesize(classContext, ("java/io/PrintStream"))));

        intrinsics.registerIntrinsic(systemDesc, "setIn", setPrintStreamDesc, setVolatile(in));
        intrinsics.registerIntrinsic(systemDesc, "setOut", setPrintStreamDesc, setVolatile(out));
        intrinsics.registerIntrinsic(systemDesc, "setErr", setPrintStreamDesc, setVolatile(err));
    }

    public static void registerJavaLangThreadIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jltDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Thread");
        MethodDescriptor returnJlt = MethodDescriptor.synthesize(classContext, jltDesc, List.of());

        StaticValueIntrinsic currentThread = (builder, owner, name, descriptor, arguments) -> builder.currentThread();

        intrinsics.registerIntrinsic(jltDesc, "currentThread", returnJlt, currentThread);
    }

    public static void registerJavaLangThrowableIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jltDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Throwable");
        ClassTypeDescriptor steDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/StackTraceElement");
        ArrayTypeDescriptor steArrayDesc = ArrayTypeDescriptor.of(classContext, steDesc);

        ZeroInitializerLiteral zero = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getSignedInteger32Type());

        // todo: temporary, until we have a stack walker

        InstanceValueIntrinsic fillInStackTrace = (builder, kind, instance, owner, name, descriptor, arguments) ->
            instance;

        InstanceValueIntrinsic getStackTrace = (builder, kind, instance, owner, name, descriptor, arguments) ->
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

        StaticValueIntrinsic divideUnsigned = (builder, owner, name, descriptor, arguments) ->
            builder.divide(asUnsigned(builder, arguments.get(0)), asUnsigned(builder, arguments.get(1)));

        StaticValueIntrinsic remainderUnsigned = (builder, owner, name, descriptor, arguments) ->
            builder.remainder(asUnsigned(builder, arguments.get(0)), asUnsigned(builder, arguments.get(1)));

        intrinsics.registerIntrinsic(integerDesc, "divideUnsigned", binaryIntDesc, divideUnsigned);
        intrinsics.registerIntrinsic(longDesc, "divideUnsigned", binaryLongDesc, divideUnsigned);

        intrinsics.registerIntrinsic(integerDesc, "remainderUnsigned", binaryIntDesc, remainderUnsigned);
        intrinsics.registerIntrinsic(longDesc, "remainderUnsigned", binaryLongDesc, remainderUnsigned);

        StaticValueIntrinsic ror = (builder, owner, name, descriptor, arguments) ->
            builder.ror(arguments.get(0), arguments.get(1));

        StaticValueIntrinsic rol = (builder, owner, name, descriptor, arguments) ->
            builder.rol(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(integerDesc, "rotateRight", binaryIntDesc, ror);
        intrinsics.registerIntrinsic(longDesc, "rotateRight", longIntDesc, ror);

        intrinsics.registerIntrinsic(integerDesc, "rotateLeft", binaryIntDesc, rol);
        intrinsics.registerIntrinsic(longDesc, "rotateLeft", longIntDesc, rol);

        StaticValueIntrinsic compare = (builder, owner, name, descriptor, arguments) ->
            builder.cmp(arguments.get(0), arguments.get(1));
        StaticValueIntrinsic compareUnsigned = (builder, owner, name, descriptor, arguments) ->
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

        StaticValueIntrinsic floatToRawIntBits = (builder, owner, name, descriptor, arguments) ->
            builder.bitCast(arguments.get(0), ts.getSignedInteger32Type());
        StaticValueIntrinsic doubleToRawLongBits = (builder, owner, name, descriptor, arguments) ->
            builder.bitCast(arguments.get(0), ts.getSignedInteger64Type());

        intrinsics.registerIntrinsic(floatDesc, "floatToRawIntBits", floatToIntMethodDesc, floatToRawIntBits);
        intrinsics.registerIntrinsic(doubleDesc, "doubleToRawLongBits", doubleToLongMethodDesc, doubleToRawLongBits);

        MethodDescriptor intToFloatMethodDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.F, List.of(BaseTypeDescriptor.I));
        MethodDescriptor longToDoubleMethodDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.D, List.of(BaseTypeDescriptor.J));

        StaticValueIntrinsic intBitsToFloat = (builder, owner, name, descriptor, arguments) ->
            builder.bitCast(arguments.get(0), ts.getFloat32Type());
        StaticValueIntrinsic longBitsToDouble = (builder, owner, name, descriptor, arguments) ->
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
        Layout layout = Layout.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");

        // Object#getClass()Ljava/lang/Class; --> field read of the "typeId" field
        MethodDescriptor getClassDesc =
            MethodDescriptor.synthesize(classContext,
                ClassTypeDescriptor.synthesize(classContext, "java/lang/Class"), List.of());
        InstanceValueIntrinsic getClassIntrinsic = (builder, kind, instance, owner, name, descriptor, arguments) ->
            builder.classOf(builder.typeIdOf(builder.referenceHandle(instance)));
        intrinsics.registerIntrinsic(classDesc, "getClass", getClassDesc, getClassIntrinsic);
    }

    static Literal literalOf(CompilationContext ctxt, boolean v) {
        return ctxt.getLiteralFactory().literalOf(v);
    }

    static void registerCcQuarkusQccRuntimeValuesIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        // descriptors

        ClassTypeDescriptor valsDesc = ClassTypeDescriptor.synthesize(classContext, "cc/quarkus/qcc/runtime/Values");
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

        StaticValueIntrinsic isConstant = (builder, owner, name, descriptor, arguments) -> literalOf(ctxt, arguments.get(0) instanceof Literal);

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

        StaticValueIntrinsic isAlwaysTrue = (builder, owner, name, descriptor, arguments) -> literalOf(ctxt, arguments.get(0) instanceof BooleanLiteral && ((BooleanLiteral) arguments.get(0)).booleanValue());
        intrinsics.registerIntrinsic(valsDesc, "isAlwaysTrue", boolBoolDesc, isAlwaysTrue);

        StaticValueIntrinsic isAlwaysFalse = (builder, owner, name, descriptor, arguments) -> literalOf(ctxt, arguments.get(0) instanceof BooleanLiteral && ((BooleanLiteral) arguments.get(0)).booleanValue());
        intrinsics.registerIntrinsic(valsDesc, "isAlwaysFalse", boolBoolDesc, isAlwaysFalse);

        // todo: compareAndSwap*

        // getAndSet*

        class GetAndSetIntrinsic implements StaticValueIntrinsic {
            private final MemoryAtomicityMode mode;

            GetAndSetIntrinsic(MemoryAtomicityMode mode) {
                this.mode = mode;
            }

            public Value emitIntrinsic(BasicBlockBuilder builder, TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments) {
                ValueHandle target = getTarget(ctxt, builder, arguments.get(0));
                if (target == null) {
                    return arguments.get(0);
                }
                return builder.getAndSet(target, arguments.get(1), mode);
            }
        }

        StaticValueIntrinsic getAndSetVolatile = new GetAndSetIntrinsic(MemoryAtomicityMode.VOLATILE);

        intrinsics.registerIntrinsic(valsDesc, "getAndSetVolatile", objObjObjDescriptor, getAndSetVolatile);
        intrinsics.registerIntrinsic(valsDesc, "getAndSetVolatile", longLongLongDescriptor, getAndSetVolatile);
        intrinsics.registerIntrinsic(valsDesc, "getAndSetVolatile", intIntIntDescriptor, getAndSetVolatile);

        // todo: determine the real atomicity mode for "relaxed"
        StaticValueIntrinsic getAndSetRelaxed = new GetAndSetIntrinsic(MemoryAtomicityMode.MONOTONIC);

        intrinsics.registerIntrinsic(valsDesc, "getAndSetRelaxed", objObjObjDescriptor, getAndSetRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "getAndSetRelaxed", longLongLongDescriptor, getAndSetRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "getAndSetRelaxed", intIntIntDescriptor, getAndSetRelaxed);

        // set*

        class SetIntrinsic implements StaticIntrinsic {
            private final MemoryAtomicityMode mode;

            SetIntrinsic(MemoryAtomicityMode mode) {
                this.mode = mode;
            }

            @Override
            public Node emitIntrinsic(BasicBlockBuilder builder, TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments) {
                ValueHandle target = getTarget(ctxt, builder, arguments.get(0));
                if (target == null) {
                    return builder.nop();
                }
                return builder.store(target, arguments.get(1), mode);
            }
        }

        // todo: determine the real atomicity mode for "relaxed"
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

        class GetIntrinsic implements StaticValueIntrinsic {
            private final MemoryAtomicityMode mode;

            GetIntrinsic(MemoryAtomicityMode mode) {
                this.mode = mode;
            }

            @Override
            public Value emitIntrinsic(BasicBlockBuilder builder, TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments) {
                ValueHandle target = getTarget(ctxt, builder, arguments.get(0));
                if (target == null) {
                    return arguments.get(0);
                }
                return builder.load(target, mode);
            }
        }

        StaticValueIntrinsic getVolatile = new GetIntrinsic(MemoryAtomicityMode.VOLATILE);

        intrinsics.registerIntrinsic(valsDesc, "getVolatile", objObjDescriptor, getVolatile);
        intrinsics.registerIntrinsic(valsDesc, "getVolatile", intIntDescriptor, getVolatile);
        intrinsics.registerIntrinsic(valsDesc, "getVolatile", longLongDescriptor, getVolatile);

        // todo: determine the real atomicity mode for "relaxed"
        StaticValueIntrinsic getRelaxed = new GetIntrinsic(MemoryAtomicityMode.MONOTONIC);

        intrinsics.registerIntrinsic(valsDesc, "getRelaxed", objObjDescriptor, getRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "getRelaxed", intIntDescriptor, getRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "getRelaxed", objObjDescriptor, getRelaxed);
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

        StaticValueIntrinsic min = (builder, owner, name, descriptor, arguments) ->
            builder.min(arguments.get(0), arguments.get(1));

        StaticValueIntrinsic max = (builder, owner, name, descriptor, arguments) ->
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
}

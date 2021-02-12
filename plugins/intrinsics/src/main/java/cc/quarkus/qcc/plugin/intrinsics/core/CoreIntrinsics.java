package cc.quarkus.qcc.plugin.intrinsics.core;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.plugin.intrinsics.InstanceValueIntrinsic;
import cc.quarkus.qcc.plugin.intrinsics.Intrinsics;
import cc.quarkus.qcc.plugin.intrinsics.StaticIntrinsic;
import cc.quarkus.qcc.plugin.intrinsics.StaticValueIntrinsic;
import cc.quarkus.qcc.plugin.layout.Layout;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.descriptor.BaseTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

/**
 * Core JDK intrinsics.
 */
public final class CoreIntrinsics {
    public static void register(CompilationContext ctxt) {
        registerJavaLangSystemIntrinsics(ctxt);
        registerJavaLangObjectIntrinsics(ctxt);
        registerJavaLangIntegerLongMathIntrinsics(ctxt);
        registerJavaLangFloatDoubleMathIntrinsics(ctxt);
    }

    private static StaticIntrinsic setVolatile(FieldElement field) {
        return (builder, owner, name, descriptor, arguments) -> builder.store(builder.staticField(field), arguments.get(0), MemoryAtomicityMode.VOLATILE);
    }

    public static void registerJavaLangSystemIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor systemDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/System");

        // Null and no-operation intrinsics

        StaticValueIntrinsic returnNull = (builder, owner, name, descriptor, arguments) ->
            builder.getCurrentElement().getEnclosingType().getContext().getCompilationContext().getLiteralFactory().literalOfNull();
        intrinsics.registerIntrinsic(systemDesc, "getSecurityManager",
            MethodDescriptor.synthesize(classContext,
                ClassTypeDescriptor.synthesize(classContext,"java/lang/SecurityManager"), List.of()),
            returnNull);

        // System public API

        ValidatedTypeDefinition jls = classContext.findDefinedType("java/lang/System").validate();
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

    public static void registerJavaLangIntegerLongMathIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        // Mathematical intrinsics

        ClassTypeDescriptor integerDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Integer");
        ClassTypeDescriptor longDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Long");

        // binary operations

        MethodDescriptor binaryIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.I, BaseTypeDescriptor.I));
        MethodDescriptor binaryLongDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.J, BaseTypeDescriptor.J));
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

        // Object#getClass()Ljava/lang/Class; --> field read of the "klass" field
        MethodDescriptor getClassDesc =
            MethodDescriptor.synthesize(classContext,
                ClassTypeDescriptor.synthesize(classContext, "java/lang/Class"), List.of());
        final FieldElement classFieldElement = layout.getObjectClassField();
        InstanceValueIntrinsic getClassIntrinsic = (builder, kind, instance, owner, name, descriptor, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(instance), classFieldElement), MemoryAtomicityMode.UNORDERED);
        intrinsics.registerIntrinsic(classDesc, "getClass", getClassDesc, getClassIntrinsic);
    }
}

package cc.quarkus.qcc.plugin.intrinsics.core;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.plugin.intrinsics.InstanceValueIntrinsic;
import cc.quarkus.qcc.plugin.intrinsics.Intrinsics;
import cc.quarkus.qcc.plugin.intrinsics.StaticIntrinsic;
import cc.quarkus.qcc.plugin.intrinsics.StaticValueIntrinsic;
import cc.quarkus.qcc.plugin.layout.Layout;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

/**
 * Core JDK intrinsics.
 */
public final class CoreIntrinsics {
    public static void register(CompilationContext ctxt) {
        registerJavaLangSystemIntrinsics(ctxt);
        registerJavaLangObjectIntrinsics(ctxt);
    }

    private static StaticIntrinsic setVolatile(FieldElement field) {
        return (builder, owner, name, descriptor, arguments) -> builder.writeStaticField(field, arguments.get(0), JavaAccessMode.VOLATILE);
    }

    public static void registerJavaLangSystemIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor systemDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/System");

        // Null and no-operation intrinsics

        StaticValueIntrinsic returnNull = (builder, owner, name, descriptor, arguments) ->
            builder.getCurrentElement().getEnclosingType().getContext().getCompilationContext().getLiteralFactory().literalOfNull();
        intrinsics.registerIntrinsic(systemDesc, "getSecurityManager",
            MethodDescriptor.parse(classContext, ByteBuffer.wrap("()Ljava/lang/SecurityManager;".getBytes(StandardCharsets.UTF_8))),
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

        MethodDescriptor setPrintStreamDesc = MethodDescriptor.parse(classContext, ByteBuffer.wrap("(Ljava/io/PrintStream;)V".getBytes(StandardCharsets.UTF_8)));

        intrinsics.registerIntrinsic(systemDesc, "setIn", setPrintStreamDesc, setVolatile(in));
        intrinsics.registerIntrinsic(systemDesc, "setOut", setPrintStreamDesc, setVolatile(out));
        intrinsics.registerIntrinsic(systemDesc, "setErr", setPrintStreamDesc, setVolatile(err));
    }

    public static void registerJavaLangObjectIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        Layout layout = Layout.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");

        // Object#getClass()Ljava/lang/Class; --> field read of the "klass" field
        MethodDescriptor getClassDesc = MethodDescriptor.parse(classContext, ByteBuffer.wrap("()Ljava/lang/Class;".getBytes(StandardCharsets.UTF_8)));
        final FieldElement classFieldElement = layout.getObjectClassField();
        InstanceValueIntrinsic getClassIntrinsic = (builder, kind, instance, owner, name, descriptor, arguments) ->
            builder.readInstanceField(instance, classFieldElement, JavaAccessMode.PLAIN);
        intrinsics.registerIntrinsic(classDesc, "getClass", getClassDesc, getClassIntrinsic);
    }
}

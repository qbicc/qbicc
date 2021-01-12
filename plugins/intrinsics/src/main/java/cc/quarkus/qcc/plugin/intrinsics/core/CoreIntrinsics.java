package cc.quarkus.qcc.plugin.intrinsics.core;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.plugin.intrinsics.Intrinsics;
import cc.quarkus.qcc.plugin.intrinsics.StaticValueIntrinsic;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

/**
 * Core JDK intrinsics.
 */
public final class CoreIntrinsics {
    public static void register(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        // Null and no-operation intrinsics

        StaticValueIntrinsic returnNull = (builder, owner, name, descriptor, arguments) ->
            builder.getCurrentElement().getEnclosingType().getContext().getCompilationContext().getLiteralFactory().literalOfNull();
        intrinsics.registerIntrinsic(ClassTypeDescriptor.synthesize(classContext, "java/lang/System"), "getSecurityManager",
            MethodDescriptor.parse(classContext, ByteBuffer.wrap("()Ljava/lang/SecurityManager;".getBytes(StandardCharsets.UTF_8))),
            returnNull);

    }
}

package cc.quarkus.qcc.plugin.main_method;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.plugin.intrinsics.Intrinsics;
import cc.quarkus.qcc.plugin.intrinsics.StaticIntrinsic;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

/**
 *
 */
public class UserMainIntrinsic implements StaticIntrinsic {
    private final MethodElement realMain;

    public UserMainIntrinsic(final MethodElement realMain) {
        this.realMain = realMain;
    }

    public Node emitIntrinsic(final BasicBlockBuilder builder, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        return builder.invokeStatic(realMain, arguments);
    }

    public static void register(CompilationContext ctxt, MethodElement mainMethod) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        TypeDescriptor runtimeMainDesc = TypeDescriptor.parseClassConstant(classContext, ByteBuffer.wrap("cc/quarkus/qcc/runtime/main/Main".getBytes(StandardCharsets.UTF_8)));
        MethodDescriptor runtimeMainMethodDesc = MethodDescriptor.parse(classContext, ByteBuffer.wrap(("([Ljava/lang/String;)V").getBytes(StandardCharsets.UTF_8)));
        intrinsics.registerIntrinsic(runtimeMainDesc, "userMain", runtimeMainMethodDesc, new UserMainIntrinsic(mainMethod));
    }
}

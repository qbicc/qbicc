package cc.quarkus.qcc.plugin.intrinsics;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

/**
 * The basic block builder which substitutes invocations of intrinsic methods.
 */
public final class IntrinsicBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public IntrinsicBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Node invokeStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(owner, name, descriptor);
        return intrinsic != null ? intrinsic.emitIntrinsic(this, owner, name, descriptor, arguments) : super.invokeStatic(owner, name, descriptor, arguments);
    }

    public Value invokeValueStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        StaticValueIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticValueIntrinsic(owner, name, descriptor);
        return intrinsic != null ? intrinsic.emitIntrinsic(this, owner, name, descriptor, arguments) : super.invokeValueStatic(owner, name, descriptor, arguments);
    }
}

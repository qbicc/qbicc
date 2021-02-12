package cc.quarkus.qcc.plugin.opt;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BitCast;
import cc.quarkus.qcc.graph.DynamicInvocationValue;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.NodeVisitor;
import cc.quarkus.qcc.graph.Value;

public class BitCastOptimizerVisitor implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock> {
    private final CompilationContext context;
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate;

    public BitCastOptimizerVisitor(final CompilationContext context, final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    public NodeVisitor<Node.Copier, Value, Node, BasicBlock> getDelegateNodeVisitor() {
        return delegate;
    }

    @Override
    public Value visit(final Node.Copier param, final BitCast node) {
        if (node.getInput() instanceof BitCast) {
            final BitCast inputNode = (BitCast) node.getInput();
            if (inputNode.getInput().getType().equals(node.getType())) {
                // BitCast(BitCast(a, x), type-of a) -> a
                return inputNode.getInput();
            }

            // BitCast(BitCast(a, x), y) -> BitCast(a, y)
            return param.getBlockBuilder().bitCast(inputNode.getInput(), node.getType());
        }

        return delegate.visit(param, node);
    }
}

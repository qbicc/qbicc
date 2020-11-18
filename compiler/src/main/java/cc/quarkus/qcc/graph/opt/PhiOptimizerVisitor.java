package cc.quarkus.qcc.graph.opt;

import java.util.Iterator;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueVisitor;

/**
 *
 */
public class PhiOptimizerVisitor implements ValueVisitor.Delegating<Node.Copier, Value> {
    private final CompilationContext context;
    private final ValueVisitor<Node.Copier, Value> delegate;

    public PhiOptimizerVisitor(final CompilationContext context, final ValueVisitor<Node.Copier, Value> delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    public ValueVisitor<Node.Copier, Value> getDelegateValueVisitor() {
        return delegate;
    }

    public Value visit(final Node.Copier param, final PhiValue node) {
        // see if there is more than one input
        Iterator<BasicBlock> iterator = param.getReachableBlocks().iterator();
        while (iterator.hasNext()) {
            BasicBlock b1 = iterator.next();
            Value v1 = node.getValueForBlock(b1);
            if (v1 != null && ! v1.equals(node)) {
                while (iterator.hasNext()) {
                    BasicBlock b2 = iterator.next();
                    Value v2 = node.getValueForBlock(b2);
                    if (v2 != null && ! v2.equals(v1) && ! v2.equals(node)) {
                        // multiple values; process as phi node
                        return (PhiValue) ValueVisitor.Delegating.super.visit(param, node);
                    }
                }
                // one value; process as specific value
                return param.copyValue(v1);
            }
        }
        // *no* inputs; should be impossible!
        context.error("internal: phi block with no inputs");
        return context.getLiteralFactory().literalOfNull();
    }
}

package org.qbicc.plugin.opt;

import java.util.Iterator;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;

/**
 * A copying visitor which removes redundant {@link PhiValue} nodes.
 */
public class PhiOptimizerVisitor implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock, ValueHandle> {
    private final CompilationContext context;
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate;

    public PhiOptimizerVisitor(final CompilationContext context, final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    public NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> getDelegateNodeVisitor() {
        return delegate;
    }

    public Value visit(final Node.Copier param, final PhiValue node) {
        // see if there is more than one input
        Iterator<BasicBlock> iterator = node.getPinnedBlock().getIncoming().iterator();
        while (iterator.hasNext()) {
            BasicBlock b1 = iterator.next();
            Terminator t1 = b1.getTerminator();
            if (b1.isReachable()) {
                Value v1 = node.getValueForInput(t1);
                if (v1 != null && ! v1.equals(node)) {
                    while (iterator.hasNext()) {
                        BasicBlock b2 = iterator.next();
                        Terminator t2 = b2.getTerminator();
                        if (b2.isReachable()) {
                            Value v2 = node.getValueForInput(t2);
                            if (v2 != null && ! v2.equals(v1) && ! v2.equals(node)) {
                                // multiple values; process as phi node
                                return (PhiValue) NodeVisitor.Delegating.super.visit(param, node);
                            }
                        }
                    }
                    // one value; process as specific value
                    return param.copyValue(v1);
                }
            }
        }
        // *no* inputs; should be impossible!
        context.error("internal: phi block with no inputs (element: " + node.getElement() + ")");
        return (PhiValue) NodeVisitor.Delegating.super.visit(param, node);
    }
}

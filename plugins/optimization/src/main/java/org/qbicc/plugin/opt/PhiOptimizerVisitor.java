package org.qbicc.plugin.opt;

import java.util.Iterator;
import java.util.Set;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.PhiValue;
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
        Set<Value> possibleValues = node.getPossibleValues();
        Iterator<Value> iterator = possibleValues.iterator();
        if (iterator.hasNext()) {
            Value first = iterator.next();
            if (iterator.hasNext()) {
                // multiple values; process as phi node
                return NodeVisitor.Delegating.super.visit(param, node);
            }
            // one value; process as specific value
            return param.copyValue(first);
        }
        // *no* inputs; should be impossible!
        context.error("internal: phi block with no inputs (element: " + node.getElement() + ")");
        return NodeVisitor.Delegating.super.visit(param, node);
    }
}

package org.qbicc.plugin.opt;

import java.util.Iterator;
import java.util.Set;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.UndefinedLiteral;

/**
 * A copying visitor which removes redundant {@link BlockParameter} nodes.
 */
public class BlockParameterOptimizingVisitor implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock> {
    private final CompilationContext context;
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate;

    public BlockParameterOptimizingVisitor(final CompilationContext context, final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    public NodeVisitor<Node.Copier, Value, Node, BasicBlock> getDelegateNodeVisitor() {
        return delegate;
    }

    public Value visit(final Node.Copier copier, final BlockParameter node) {
        Set<Value> possibleValues = node.getPossibleValues();
        Iterator<Value> iterator = possibleValues.iterator();
        if (iterator.hasNext()) {
            Value first = iterator.next();
            if (iterator.hasNext() || first == node || first instanceof UndefinedLiteral) {
                // multiple values, or it is an input parameter; process as normal
                return NodeVisitor.Delegating.super.visit(copier, node);
            }
            // one value; process as specific value
            return copier.copyValue(first);
        }
        // *no* inputs; should be impossible!
        context.error("internal: block parameter with no inputs (element: " + node.element() + ")");
        return NodeVisitor.Delegating.super.visit(copier, node);
    }
}

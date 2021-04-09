package org.qbicc.plugin.opt;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.Goto;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;

/**
 * A copier which removes all goto nodes that are only entered by one entry block.
 */
public class GotoRemovingVisitor implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock, ValueHandle> {
    private final CompilationContext context;
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate;
    private final Set<BasicBlock> deleted = new HashSet<>();

    public GotoRemovingVisitor(final CompilationContext context, final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    public NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> getDelegateNodeVisitor() {
        return delegate;
    }

    public BasicBlock visit(final Node.Copier param, final Goto node) {
        BasicBlock target = node.getResumeTarget();
        if (target.getIncoming().size() == 1 && Objects.equals(node.getCallSite(), target.getTerminator().getCallSite())) {
            // delete the goto target and fold it into the current block
            deleted.add(target);
            return param.copyTerminator(target.getTerminator());
        } else {
            return getDelegateTerminatorVisitor().visit(param, node);
        }
    }

    public Value visit(final Node.Copier param, final PhiValue node) {
        if (deleted.contains(node.getPinnedBlock())) {
            // the deleted block only has one incoming block, so the phi must also have only one valid incoming value
            return param.copyValue(node.getValueForInput(node.getPinnedBlock().getIncoming().iterator().next().getTerminator()));
        } else {
            return getDelegateValueVisitor().visit(param, node);
        }
    }

    public Node visit(final Node.Copier param, final BlockEntry node) {
        // the block being entered
        BasicBlock block = node.getPinnedBlock();
        if (deleted.contains(block)) {
            // prepend the deleted predecessors' nodes
            return param.copyNode(block.getIncoming().iterator().next().getTerminator().getDependency());
        }
        return getDelegateActionVisitor().visit(param, node);
    }
}

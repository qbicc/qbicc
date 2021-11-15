package org.qbicc.plugin.opt;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.Goto;
import org.qbicc.graph.If;
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

    @Override
    public BasicBlock visit(final Node.Copier param, final If node) {
        // try to find diamond constructs of this form:
        //
        //         If(cond, a, b)
        //        [T]/         \[F]
        //  a: BlockEntry  b: BlockEntry
        //     Goto(c)[g1]    Goto(c)[g2]
        //          \          /
        //         c: BlockEntry
        //              [...]
        //       [c] Phi(a: x, b: y)
        //
        // ...and replace them inline with:
        //
        //      Select(cond, x, y)
        //
        // ...by treating the If as a Goto to be deleted.

        // determine if the shape matches.
        if (node.getTrueBranch().getTerminator() instanceof Goto g1
            && node.getFalseBranch().getTerminator() instanceof Goto g2
            && g1.getResumeTarget() == g2.getResumeTarget()
            && g1.getDependency() instanceof BlockEntry
            && g2.getDependency() instanceof BlockEntry
            && node.getTrueBranch().getIncoming().size() == 1
            && node.getFalseBranch().getIncoming().size() == 1
            && g1.getResumeTarget().getIncoming().size() == 2
        ) {
            // either branch works, because we want the successor's successor
            final BasicBlock tb = node.getTrueBranch();
            // delete the if's successor's successor and fold it into the current block
            final BasicBlock successor = tb.getTerminator().getSuccessor(0);
            deleted.add(successor);
            return param.copyTerminator(successor.getTerminator());
        } else {
            return getDelegateTerminatorVisitor().visit(param, node);
        }
    }

    public Value visit(final Node.Copier param, final PhiValue node) {
        final BasicBlock pinnedBlock = node.getPinnedBlock();
        final Set<BasicBlock> incoming = pinnedBlock.getIncoming();
        final boolean delete = deleted.contains(pinnedBlock);
        if (incoming.size() == 2 && delete) {
            // This is the `phi` part of a diamond.  This `If` is the sole predecessor of either of the predecessors of the pinned block.
            final If if_ = (If) incoming.iterator().next().getIncoming().iterator().next().getTerminator();
            Value trueValue = param.copyValue(node.getValueForInput(if_.getTrueBranch().getTerminator()));
            Value falseValue = param.copyValue(node.getValueForInput(if_.getFalseBranch().getTerminator()));
            return param.getBlockBuilder().select(param.copyValue(if_.getCondition()), trueValue, falseValue);
        } else if (delete) {
            assert incoming.size() == 1;
            // the deleted block only has one incoming block, so the phi must also have only one valid incoming value
            return param.copyValue(node.getValueForInput(incoming.iterator().next().getTerminator()));
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

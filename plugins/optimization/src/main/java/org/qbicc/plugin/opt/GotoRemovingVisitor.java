package org.qbicc.plugin.opt;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.Goto;
import org.qbicc.graph.If;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;

/**
 * A copier which removes all goto nodes that are only entered by one entry block.
 */
public class GotoRemovingVisitor implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock> {
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate;
    private final Set<BasicBlock> deleted = new HashSet<>();

    public GotoRemovingVisitor(final CompilationContext context, final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate) {
        this.delegate = delegate;
    }

    public NodeVisitor<Node.Copier, Value, Node, BasicBlock> getDelegateNodeVisitor() {
        return delegate;
    }

    public BasicBlock visit(final Node.Copier param, final Goto node) {
        BasicBlock target = node.getResumeTarget();
        if (target.getIncoming().size() == 1 && Objects.equals(node.callSite(), target.getTerminator().callSite())) {
            // delete the goto target and fold it into the current block
            deleted.add(target);
            param.copyNode(node.getDependency());
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
        //
        // ...and replace them inline with:
        //
        //      Select(cond, x, y)
        //
        // ...by treating the If as a Goto to be deleted.

        // determine if the shape matches.
        // TODO:
        //   Temporarily disabled for correctness until we have an easy way to probe the schedule.
        //   This is necessary because there might be non-movable constraint nodes scheduled to the branch blocks and we currently have no way to detect them.
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (false && node.getTrueBranch().getTerminator() instanceof Goto g1
            && node.getFalseBranch().getTerminator() instanceof Goto g2
            && g1.getResumeTarget() == g2.getResumeTarget()
            && g1.getDependency() instanceof BlockEntry // TODO: replace with e.g. g1.getBlockSchedule().getValueCount() == 0
            && g2.getDependency() instanceof BlockEntry // TODO: replace with e.g. g2.getBlockSchedule().getValueCount() == 0
            && node.getTrueBranch().getIncoming().size() == 1
            && node.getFalseBranch().getIncoming().size() == 1
            && g1.getResumeTarget().getIncoming().size() == 2
            && Objects.equals(node.callSite(), g1.getResumeTarget().getTerminator().callSite())
            && Objects.equals(node.callSite(), g2.getResumeTarget().getTerminator().callSite())
        ) {
            // either branch works, because we want the successor's successor
            final BasicBlock tb = node.getTrueBranch();
            // delete the two branches and fold them into the current block
            deleted.add(node.getTrueBranch());
            deleted.add(node.getFalseBranch());
            // delete the if's successor's successor and fold it into the current block
            final BasicBlock successor = tb.getTerminator().getSuccessor(0);
            deleted.add(successor);
            param.copyNode(node.getDependency());
            return param.copyTerminator(successor.getTerminator());
        } else {
            return getDelegateTerminatorVisitor().visit(param, node);
        }
    }

    @Override
    public Value visit(Node.Copier copier, BlockParameter node) {
        BasicBlock block = node.getPinnedBlock();
        if (deleted.contains(block)) {
            Slot slot = node.getSlot();
            Set<BasicBlock> incoming = block.getIncoming();
            if (incoming.size() == 1) {
                // it was the result of a deleted `goto`; replace with literal incoming value
                BasicBlock gotoBlock = incoming.iterator().next();
                Goto goto_ = (Goto) gotoBlock.getTerminator();
                return copier.copyValue(goto_.getOutboundArgument(slot));
            } else if (incoming.size() == 2) {
                // it was the result of a deleted `if`; replace with a select
                BasicBlock ifBlock = incoming.iterator().next().getIncoming().iterator().next();
                If if_ = (If) ifBlock.getTerminator();
                Value copiedCond = copier.copyValue(if_.getCondition());
                BasicBlock tb = if_.getTrueBranch();
                BasicBlock fb = if_.getFalseBranch();
                return copier.getBlockBuilder().select(
                    copiedCond,
                    copier.copyValue(tb.getTerminator().getOutboundArgument(slot)),
                    copier.copyValue(fb.getTerminator().getOutboundArgument(slot))
                );
            } else {
                // breakpoint
                throw new IllegalStateException();
            }
        } else {
            return getDelegateValueVisitor().visit(copier, node);
        }
    }

    public Node visit(final Node.Copier param, final BlockEntry node) {
        // the block being entered
        BasicBlock block = node.getPinnedBlock();
        if (deleted.contains(block)) {
            if (block.getIncoming().size() == 2) {
                // prepend the deleted predecessor's predecessors' nodes
                return param.copyNode(block.getIncoming().iterator().next().getIncoming().iterator().next().getTerminator().getDependency());
            } else {
                // prepend the deleted predecessors' nodes
                return param.copyNode(block.getIncoming().iterator().next().getTerminator().getDependency());
            }
        }
        return getDelegateActionVisitor().visit(param, node);
    }
}

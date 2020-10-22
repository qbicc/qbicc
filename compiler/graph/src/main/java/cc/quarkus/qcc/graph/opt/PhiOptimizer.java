package cc.quarkus.qcc.graph.opt;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import cc.quarkus.qcc.graph.Action;
import cc.quarkus.qcc.graph.ActionVisitor;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.NodeVisitor;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.TerminatorVisitor;
import cc.quarkus.qcc.graph.Triable;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueVisitor;
import io.smallrye.common.constraint.Assert;

/**
 * A processor which removes redundant phi nodes.
 */
public class PhiOptimizer {
    private final BasicBlockBuilder gf;
    private final Visitor visitor = new Visitor();

    public PhiOptimizer(final BasicBlockBuilder gf) {
        this.gf = gf;
    }

    public BasicBlock transform(BasicBlock input) {
        BasicBlock output;
        int pass = 1;
        for (;;) {
            Context ctxt = new Context(input.calculateReachableBlocks());
            visitor.copy(ctxt, input);
            while (! ctxt.q.isEmpty()) {
                BasicBlock block = ctxt.q.remove();
                gf.begin(ctxt.blockMapping.get(block));
                visitor.copy(ctxt, block.getTerminator());
            }
            while (! ctxt.phiQ.isEmpty()) {
                PhiValue orig = ctxt.phiQ.remove();
                PhiValue copy = (PhiValue) ctxt.nodeMapping.get(orig);
                // now process and map all incoming values over again
                for (final BasicBlock block : ctxt.reachable) {
                    Value val = orig.getValueForBlock(block);
                    if (val != null) {
                        copy.setValueForBlock(visitor.copy(ctxt, block), visitor.copy(ctxt, val));
                    }
                }
            }
            output = BlockLabel.getTargetOf(visitor.copy(ctxt, input));
            if (! ctxt.changed) {
                output = input;
                break;
            }
            // try again
            input = output;
            pass ++;
        }
        return output;
    }

    static final class Context {
        final Map<Node, Node> nodeMapping = new HashMap<>();
        final Map<BasicBlock, BlockLabel> blockMapping = new HashMap<>();
        final Set<BasicBlock> reachable;
        final Queue<BasicBlock> q;
        final Queue<PhiValue> phiQ = new ArrayDeque<>();
        boolean changed;

        Context(final Set<BasicBlock> reachable) {
            this.reachable = reachable;
            q = new ArrayDeque<>(reachable.size());
        }
    }

    final class Visitor implements ValueVisitor.Copying<Context>, TerminatorVisitor.Copying<Context>, ActionVisitor.Copying<Context>, NodeVisitor<Context, Node> {
        public BasicBlockBuilder getBuilder(final Context param) {
            return gf;
        }

        public BlockLabel copy(final Context param, final BasicBlock block) {
            BlockLabel label = param.blockMapping.get(block);
            if (label != null) {
                return label;
            }
            param.blockMapping.put(block, label = new BlockLabel());
            param.q.add(block);
            return label;
        }

        public BasicBlock copy(final Context param, final Terminator original) {
            BasicBlock block = TerminatorVisitor.Copying.super.copy(param, original);
            // process successors
            int cnt = original.getSuccessorCount();
            for (int i = 0; i < cnt; i ++) {
                copy(param, original.getSuccessor(i));
            }
            return block;
        }

        public Value copy(final Context param, final Value original) {
            Node copy = param.nodeMapping.get(original);
            if (copy == null) {
                copy = ValueVisitor.Copying.super.copy(param, original);
                param.nodeMapping.put(original, copy);
            }
            return (Value) copy;
        }

        public Node copy(final Context param, final Action original) {
            Node copy = param.nodeMapping.get(original);
            if (copy == null) {
                copy = ActionVisitor.Copying.super.copy(param, original);
                param.nodeMapping.put(original, copy);
            }
            return copy;
        }

        public Triable copy(final Context param, final Triable original) {
            return (Triable) original.accept(this, param);
        }

        public Node copy(final Context param, final Node original) {
            return original.accept(this, param);
        }

        public Node visit(final Context param, final Action node) {
            return copy(param, node);
        }

        public Node visit(final Context param, final Value node) {
            return copy(param, node);
        }

        public Node visitUnknown(final Context param, final Action node) {
            throw Assert.unreachableCode();
        }

        public Node visitUnknown(final Context param, final Node node) {
            throw Assert.unreachableCode();
        }

        public BasicBlock visitUnknown(final Context param, final Terminator node) {
            throw Assert.unreachableCode();
        }

        public Value visitUnknown(final Context param, final Value node) {
            throw Assert.unreachableCode();
        }

        public Value visit(final Context param, final PhiValue node) {
            // see if there is more than one input
            Iterator<BasicBlock> iterator = param.reachable.iterator();
            while (iterator.hasNext()) {
                BasicBlock b1 = iterator.next();
                Value v1 = node.getValueForBlock(b1);
                if (v1 != null && ! v1.equals(node)) {
                    while (iterator.hasNext()) {
                        BasicBlock b2 = iterator.next();
                        Value v2 = node.getValueForBlock(b2);
                        if (v2 != null && ! v2.equals(v1) && ! v2.equals(node)) {
                            // multiple values; process as phi node
                            PhiValue copy = (PhiValue) ValueVisitor.Copying.super.visit(param, node);
                            // add early to avoid looping problems
                            param.nodeMapping.put(node, copy);
                            param.phiQ.add(node);
                            return copy;
                        }
                    }
                    param.changed = true;
                    // one value; process as specific value
                    return copy(param, v1);
                }
            }
            // *no* inputs; should be impossible!
            throw new IllegalStateException("Phi node with no inputs");
        }
    }
}

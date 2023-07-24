package org.qbicc.graph.schedule;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import io.smallrye.common.constraint.Assert;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.Invoke;
import org.qbicc.graph.Node;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.PinnedNode;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Unschedulable;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.Literal;
import org.qbicc.type.ReferenceType;

/**
 * The scheduler.
 */
public final class Scheduler {
    private final Mode mode;

    public Scheduler(Mode mode) {
        this.mode = Assert.checkNotNullParam("mode", mode);
    }

    /**
     * Schedule all of the method's nodes, starting with the given entry block.
     * After this method is called, each reachable block in the method body will have an ordered instruction list.
     *
     * @param entryBlock the entry block (must not be {@code null})
     */
    public void schedule(BasicBlock entryBlock) {
        new Context(Assert.checkNotNullParam("entryBlock", entryBlock)).run();
    }

    private final class Context {
        // input arguments
        private final BasicBlock entryBlock;

        // calculated in-progress state
        private final BlockInfo rootBlock;
        private final BlockInfo[] allBlocks;
        private final Map<BasicBlock, BlockInfo> blockInfos;
        private final Map<Node, BlockInfo> earliestMapping = new HashMap<>();
        private final Map<Node, BlockInfo> lateMapping = new HashMap<>();
        private final Map<Node, Set<Node>> dependents = new HashMap<>();
        private final Map<Set<Value>, Set<Value>> valueSetCache = new HashMap<>();

        Context(final BasicBlock entryBlock) {
            this.entryBlock = entryBlock;
            int[] indexHolder = new int[] { 2 };
            Map<BasicBlock, BlockInfo> blockInfos = new HashMap<>();
            // Assign numeric indices for each block
            BlockInfo root = new BlockInfo(entryBlock, 1);
            root.computeIndices(blockInfos, indexHolder);
            final int maxOneBasedIndex = indexHolder[0];
            BlockInfo[] allBlocks = new BlockInfo[maxOneBasedIndex - 1];
            // Map blocks into the array
            for (Map.Entry<BasicBlock, BlockInfo> entry : blockInfos.entrySet()) {
                final BlockInfo blockInfo = entry.getValue();
                allBlocks[blockInfo.index - 1] = blockInfo;
                entry.getKey().setIndex(blockInfo.index);
            }
            this.blockInfos = blockInfos;
            this.rootBlock = root;
            this.allBlocks = allBlocks;
        }

        void run() {
            // Execute algorithm to get dominators mapping
            new DominatorFinder(allBlocks).main();
            // Find the dominator tree depths
            for (BlockInfo block : allBlocks) {
                block.findDomDepths(allBlocks);
            }
            // now, use the dominator depths to calculate the schedule
            Map<Node, BlockInfo> scheduleToUse;
            scheduleEarly();
            // check mode...
            if (mode == Mode.LATE) {
                scheduleLate();
                scheduleToUse = lateMapping;
            } else {
                scheduleToUse = earliestMapping;
            }
            // assign nodes to their blocks
            for (Map.Entry<Node, BlockInfo> entry : scheduleToUse.entrySet()) {
                entry.getKey().setScheduledBlock(entry.getValue().block);
            }
            // and build the sequence
            buildSequence();
        }

        // 🌼🌼🌼🌼🌼🌼🌼🌼🌼🌼
        // Schedule early
        // 🌼🌼🌼🌼🌼🌼🌼🌼🌼🌼

        private void scheduleEarly() {
            scheduleEarly(entryBlock);
        }

        private BlockInfo scheduleEarly(BasicBlock block) {
            return scheduleEarly(block.getTerminator());
        }

        private BlockInfo scheduleDependenciesEarly(Node node) {
            BlockInfo selected = rootBlock;
            int cnt = node.getValueDependencyCount();
            for (int i = 0; i < cnt; i ++) {
                Value valueDependency = node.getValueDependency(i);
                if (valueDependency == null) {
                    throw new IllegalStateException("Missing dependency on node");
                }
                BlockInfo candidate = scheduleEarly(node, valueDependency);
                if (candidate.domDepth > selected.domDepth) {
                    selected = candidate;
                }
            }
            if (node instanceof OrderedNode on) {
                Node dependency = on.getDependency();
                BlockInfo candidate = scheduleEarly(on, dependency);
                if (candidate.domDepth > selected.domDepth) {
                    selected = candidate;
                }
                if (node instanceof Terminator t) {
                    // schedule all outbound values to blocks; we reduce the set when we build the sequence
                    for (Slot slot : t.getOutboundArgumentNames()) {
                        // make sure that the argument does not get scheduled after this terminator
                        scheduleEarly(t, t.getOutboundArgument(slot));
                    }
                }
            }
            return selected;
        }

        private BlockInfo scheduleEarly(final Node dependentNode, final Node dependencyNode) {
            if (! (dependencyNode instanceof Unschedulable)) {
                // dependentNode depends-on dependencyNode (which is to be scheduled)
                dependents.computeIfAbsent(dependencyNode, Context::newSet).add(dependentNode);
            }
            return scheduleEarly(dependencyNode);
        }

        private BlockInfo scheduleEarly(Node node) {
            BlockInfo selected = earliestMapping.get(node);
            if (selected != null) {
                return selected;
            }
            if (node instanceof PinnedNode pn) {
                return scheduleEarlyToPinnedBlock(pn, pn.getPinnedBlock());
            } else if (node instanceof Terminator t) {
                selected = scheduleEarlyToPinnedBlock(t, t.getTerminatedBlock());
                int sc = t.getSuccessorCount();
                for (int i = 0; i < sc; i ++) {
                    scheduleEarly(t.getSuccessor(i));
                }
                return selected;
            } else if (node instanceof Unschedulable un) {
                // always considered available; do not schedule (but do schedule dependencies)
                return scheduleDependenciesEarly(un);
            } else {
                selected = scheduleDependenciesEarly(node);
                earliestMapping.put(node, selected);
                return selected;
            }
        }

        private BlockInfo scheduleEarlyToPinnedBlock(final Node node, final BasicBlock pinnedBlock) {
            BlockInfo selected;
            // pinned to a block; always select that block.
            selected = blockInfos.get(pinnedBlock);
            if (selected == null) {
                throw new IllegalStateException("No block selected");
            }
            earliestMapping.put(node, selected);
            scheduleDependenciesEarly(node);
            // all dependencies have been scheduled
            return selected;
        }

        // 🌙🌙🌙🌙🌙🌙🌙🌙🌙🌙
        // Schedule late
        // 🌙🌙🌙🌙🌙🌙🌙🌙🌙🌙

        private void scheduleLate() {
            for (Node node : earliestMapping.keySet()) {
                scheduleLate(node);
            }
        }

        private BlockInfo scheduleLate(final Node node) {
            assert node != null;
            BlockInfo selected = lateMapping.get(node);
            if (selected != null) {
                return selected;
            }
            if (node instanceof Unschedulable) {
                return null;
            }
            // find the latest block which dominates all uses without pushing hoisted values into loops
            BlockInfo earliest;
            if (node instanceof Terminator t) {
                earliest = blockInfos.get(t.getTerminatedBlock());
                lateMapping.put(node, earliest);
            } else {
                if (node instanceof PinnedNode pn) {
                    earliest = blockInfos.get(pn.getPinnedBlock());
                    lateMapping.put(node, earliest);
                } else {
                    earliest = earliestMapping.get(node);
                }
            }
            if (earliest == null) {
                throw new IllegalStateException();
            }
            // schedule each dependent (some nodes have no dependents, like Return, Throw etc.)
            Set<Node> dependents = this.dependents.getOrDefault(node, Set.of());
            BlockInfo candidate;
            for (Node dependent : dependents) {
                // get the block which `dependent` is scheduled to
                candidate = scheduleLate(dependent);
                if (candidate == selected) {
                    // short-circuit trivial case
                    continue;
                }
                if (candidate == earliest) {
                    selected = earliest;
                    // still, schedule all dependents
                    continue;
                }
                if (selected == null) {
                    // first one
                    selected = candidate;
                } else {
                    // find the latest dominator of `candidate` which does not increase the number of loops (decreasing is OK)
                    while (! earliest.block.getLoops().containsAll(candidate.block.getLoops())) {
                        // would introduce new loops!
                        candidate = allBlocks[candidate.dominator - 1]; // index is one-based; array is zero-based
                    }
                    // find the latest dominator of `latest` which also dominates `candidate`
                    while (! selected.dominates(allBlocks, candidate)) {
                        // our selected block must dominate all uses
                        selected = allBlocks[selected.dominator - 1]; // index is one-based; array is zero-based
                    }
                }
            }
            if (selected != null) {
                for (Node dependent : dependents) {
                    BlockInfo dependentBlock = scheduleLate(dependent);
                    if (!selected.dominates(allBlocks, dependentBlock)) {
                        throw new AssertionError();
                    }
                }
            }
            // override selection when the block is pinned
            if (node instanceof Terminator terminator) {
                // the earliest value is already correct
                selected = earliest;
                // avoid recursion issues
                lateMapping.put(node, selected);
                // schedule (again) all outbound values to blocks
                for (Slot slot : terminator.getOutboundArgumentNames()) {
                    scheduleLate(terminator.getOutboundArgument(slot));
                }
                // recurse into successors
                for (int i = 0; i < terminator.getSuccessorCount(); i ++) {
                    scheduleLate(terminator.getSuccessor(i).getBlockEntry());
                }
            } else if (node instanceof PinnedNode) {
                // the earliest value is already correct
                selected = earliest;
            } else {
                if (selected == null) {
                    // not scheduled?
                    throw new IllegalStateException();
                }
            }
            lateMapping.put(node, selected);
            return selected;
        }

        // Sequence

        private void buildSequence() {
            // build the final sequence of instructions with entry at the top and terminator at the bottom
            Map<BasicBlock, List<Node>> blockToNodesMap = new HashMap<>(allBlocks.length);
            Map<BasicBlock, Map<Slot, BlockParameter>> blockParameters = new HashMap<>(allBlocks.length);
            Set<Node> visited = new HashSet<>();
            for (BlockInfo bi : allBlocks) {
                BlockEntry blockEntry = bi.block.getBlockEntry();
                ArrayList<Node> list = new ArrayList<>();
                list.add(blockEntry);
                visited.add(blockEntry);
                blockToNodesMap.put(bi.block, list);
            }
            ArrayDeque<BlockParameter> cleanups = new ArrayDeque<>();
            buildSequence(entryBlock.getTerminator(), visited, blockToNodesMap, blockParameters, cleanups);
            for (BlockParameter bp = cleanups.pollFirst(); bp != null; bp = cleanups.pollFirst()) {
                BasicBlock bpBlock = bp.getPinnedBlock();
                // ensure all incoming are in the schedule, at the bottom if nowhere else
                for (BasicBlock incoming : bpBlock.getIncoming()) {
                    Terminator t = incoming.getTerminator();
                    Slot slot = bp.getSlot();
                    // skip all implicit/"magical" slot names like `result` or `thrown` on invoke
                    if (t.getOutboundArgumentNames().contains(slot)) {
                        final Value outboundArgument = t.getOutboundArgument(slot);
                        buildSequence(outboundArgument, visited, blockToNodesMap, blockParameters, cleanups);
                    }
                }
            }
            for (BlockInfo bi : allBlocks) {
                BasicBlock block = bi.block;
                List<Node> list = blockToNodesMap.get(block);
                Terminator t = block.getTerminator();
                t.setScheduleIndex(list.size());
                list.add(t);
                block.setInstructions(list);
                block.setUsedParameters(Map.copyOf(blockParameters.getOrDefault(block, Map.of())));
            }
            // finally, go back and build the live-out sets
            computeLiveSetsByUse();
            Set<Value> live = new HashSet<>();
            for (BlockInfo bi : allBlocks) {
                BasicBlock block = bi.block;
                live.clear();
                live.addAll(bi.liveOut);
                Set<Value> liveOut = Util.getCachedSet(valueSetCache, bi.liveOut);
                // set the live-in and live-out sets of each node
                final List<Node> instructions = block.getInstructions();
                ListIterator<Node> li = instructions.listIterator(instructions.size());
                while (li.hasPrevious()) {
                    Node node = li.previous();
                    node.setLiveOuts(liveOut);
                    // remove this value if needed
                    if (node instanceof BlockParameter bp && bp.getPinnedBlock() == block) {
                        // keep it alive to the start of the block.
                    } if (node instanceof Value v) {
                        // this is where it was defined, thus we can remove it from the live set.
                        live.remove(v);
                    } else if (node instanceof Invoke inv) {
                        // special case!
                        live.remove(inv.getReturnValue());
                    }
                    // add any values consumed by this node to the live set
                    final int cnt = node.getValueDependencyCount();
                    for (int i = 0; i < cnt; i ++) {
                        final Value val = node.getValueDependency(i);
                        if (! (val instanceof Literal)) {
                            live.add(val);
                        }
                    }
                    // the live-out set of the previous node is the live-in set of this node
                    liveOut = Util.getCachedSet(valueSetCache, live);
                    node.setLiveIns(liveOut);
                }
            }
        }

        private void buildSequence(final Node node, final Set<Node> visited, final Map<BasicBlock, List<Node>> sequences, final Map<BasicBlock, Map<Slot, BlockParameter>> blockParameters, final ArrayDeque<BlockParameter> cleanups) {
            if (visited.add(node)) {
                if (node instanceof OrderedNode on) {
                    buildSequence(on.getDependency(), visited, sequences, blockParameters, cleanups);
                }
                if (node instanceof BlockParameter bp) {
                    BasicBlock bpBlock = bp.getPinnedBlock();
                    blockParameters.computeIfAbsent(bpBlock, Context::newMap).put(bp.getSlot(), bp);
                    cleanups.addLast(bp);
                }
                int cnt = node.getValueDependencyCount();
                for (int i = 0; i < cnt; i ++) {
                    buildSequence(node.getValueDependency(i), visited, sequences, blockParameters, cleanups);
                }
                if (node instanceof Terminator t) {
                    cnt = t.getSuccessorCount();
                    for (int i = 0; i < cnt; i ++) {
                        buildSequence(t.getSuccessor(i).getTerminator(), visited, sequences, blockParameters, cleanups);
                    }
                } else if (! (node instanceof Unschedulable)) {
                    BasicBlock targetBlock = node.getScheduledBlock();
                    if (targetBlock == null) {
                        // breakpoint
                        throw new IllegalStateException();
                    }
                    List<Node> list = sequences.get(targetBlock);
                    if (list == null) {
                        // breakpoint
                        throw new IllegalStateException();
                    }
                    node.setScheduleIndex(list.size());
                    list.add(node);
                }
            }
        }

        private void upAndMark(int blockIdx, Value value) {
            final BlockInfo bi = allBlocks[blockIdx - 1];
            final BasicBlock b = bi.block;
            if (value.getScheduledBlock() == b && ! (value instanceof BlockParameter)) {
                // killed in the block
                return;
            }
            if (bi.liveIn.contains(value)) {
                // propagation already done
                return;
            }
            bi.liveIn.add(value);
            if (value instanceof BlockParameter bp && bp.getPinnedBlock() == b) {
                // do not propagate ϕ defs
                return;
            }
            for (BasicBlock incoming : b.getIncoming()) {
                final int pi = incoming.getIndex();
                BlockInfo p = allBlocks[pi - 1];
                p.liveOut.add(value);
                upAndMark(pi, value);
            }
        }

        private void computeLiveSetsByUse() {
            Set<Value> used = new HashSet<>();
            for (BlockInfo bi : allBlocks) {
                final BasicBlock b = bi.block;
                // find all outbound arguments that are used in successor blocks
                final Terminator t = b.getTerminator();
                int cnt = t.getSuccessorCount();
                for (int i = 0; i < cnt; i ++) {
                    final BasicBlock successor = t.getSuccessor(i);
                    for (Slot slot : successor.getUsedParameterSlots()) {
                        if (! t.isImplicitOutboundArgument(slot, successor)) {
                            final Value out = t.getOutboundArgument(slot);
                            if (! (out instanceof Literal)) {
                                if (bi.liveOut.add(out)) {
                                    upAndMark(b.getIndex(), out);
                                }
                            }
                        }
                    }
                }
                for (Node node : b.getInstructions()) {
                    cnt = node.getValueDependencyCount();
                    for (int i = 0; i < cnt; i ++) {
                        final Value dep = node.getValueDependency(i);
                        if (! (dep instanceof Literal)) {
                            if ((! (dep instanceof BlockParameter bp) || bp.getPinnedBlock() != b) && used.add(dep)) {
                                upAndMark(b.getIndex(), dep);
                            }
                        }
                    }
                }
                used.clear();
            }
        }

        private static <K, V> Map<K, V> newMap(final Object ignored) {
            return new HashMap<>();
        }

        private static <E> Set<E> newSet(final Object ignored) {
            return new HashSet<>();
        }
    }

    public enum Mode {
        EARLY,
        LATE,
        ;
    }
}

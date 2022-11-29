package org.qbicc.graph.schedule;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.smallrye.common.constraint.Assert;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.DebugAddressDeclaration;
import org.qbicc.graph.DebugValueDeclaration;
import org.qbicc.graph.Node;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.PinnedNode;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Unschedulable;
import org.qbicc.graph.Value;
import org.qbicc.type.definition.element.LocalVariableElement;

/**
 * A linear schedule for basic block instructions.
 */
public interface Schedule {

    /**
     * Get the basic block for the given node.
     *
     * @param node the node to look up (must not be {@code null})
     * @return the basic block for the node, or {@code null} if the node is not scheduled
     */
    BasicBlock getBlockForNode(Node node);

    /**
     * Convenience method returning the set of local variables referenced by the final schedule.
     *
     * @return the set of local variables
     */
    Set<LocalVariableElement> getReferencedLocalVariables();

    /**
     * Create a schedule for the method whose entry block is the given block.
     *
     * @param entryBlock the entry block
     * @return a schedule for the entry block of the method
     */
    static Schedule forMethod(BasicBlock entryBlock) {
        // Simplified algorithm which simply finds *any* valid schedule.
        // todo: Find an optimized schedule.

        int[] indexHolder = new int[] { 2 };
        Map<BasicBlock, BlockInfo> blockInfos = new HashMap<>();
        // 1. First, assign numeric indices for each block
        BlockInfo root = new BlockInfo(entryBlock, 1);
        root.computeIndices(blockInfos, indexHolder);
        final int maxOneBasedIndex = indexHolder[0];
        BlockInfo[] allBlocks = new BlockInfo[maxOneBasedIndex - 1];
        // a. Map blocks into the array
        for (Map.Entry<BasicBlock, BlockInfo> entry : blockInfos.entrySet()) {
            final BlockInfo blockInfo = entry.getValue();
            allBlocks[blockInfo.index - 1] = blockInfo;
            entry.getKey().setIndex(blockInfo.index);
        }
        // 2. Now execute algorithm to get dominators mapping
        new DominatorFinder(allBlocks).main();
        // 3. Find the dominator tree depths.
        for (BlockInfo block : allBlocks) {
            block.findDomDepths(allBlocks);
        }

        // now, use the dominator depths to calculate the simplest possible schedule.
        Map<Node, BlockInfo> scheduledNodes = new LinkedHashMap<>();
        scheduleEarly(root, blockInfos, scheduledNodes, entryBlock);
        for (Map.Entry<Node, BlockInfo> entry : scheduledNodes.entrySet()) {
            entry.getKey().setScheduledBlock(entry.getValue().block);
        }
        // now build the final sequence of instructions with entry at the top and terminator at the bottom
        Map<BasicBlock, List<Node>> blockToNodesMap = new HashMap<>(allBlocks.length);
        Map<BasicBlock, Map<Slot, BlockParameter>> blockParameters = new HashMap<>(allBlocks.length);
        Set<LocalVariableElement> locals = new HashSet<>();
        Set<Node> visited = new HashSet<>();
        for (BlockInfo bi : allBlocks) {
            BlockEntry blockEntry = bi.block.getBlockEntry();
            ArrayList<Node> list = new ArrayList<>();
            list.add(blockEntry);
            visited.add(blockEntry);
            blockToNodesMap.put(bi.block, list);
        }
        ArrayDeque<BlockParameter> cleanups = new ArrayDeque<>();
        buildSequence(entryBlock.getTerminator(), visited, blockToNodesMap, blockParameters, locals, cleanups);
        for (BlockParameter bp = cleanups.pollFirst(); bp != null; bp = cleanups.pollFirst()) {
            BasicBlock bpBlock = bp.getPinnedBlock();
            // ensure all incoming are in the schedule, at the bottom if nowhere else
            for (BasicBlock incoming : bpBlock.getIncoming()) {
                Terminator t = incoming.getTerminator();
                Slot slot = bp.getSlot();
                // skip all implicit/"magical" slot names like `result` or `thrown` on invoke
                if (t.getOutboundArgumentNames().contains(slot)) {
                    buildSequence(t.getOutboundArgument(slot), visited, blockToNodesMap, blockParameters, locals, cleanups);
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

        return new Schedule() {
            public BasicBlock getBlockForNode(final Node node) {
                Assert.assertFalse(node instanceof Unschedulable);
                return node.getScheduledBlock();
            }

            public Set<LocalVariableElement> getReferencedLocalVariables() {
                return locals;
            }
        };
    }

    /**
     * Build the instruction sequence.
     * This is a DFS of all non-visited and non-terminator nodes, using the scheduler algorithm output to assign blocks.
     * This causes the actual sequencing of instructions to be independent of the scheduling algorithm or policy.
     * Note that entry nodes are all marked as visited already, being at the start of each block's list.
     *
     * @param node the node to register
     * @param visited the set of visited nodes
     * @param sequences the outbound sequence of instructions for each block which is being built
     * @param blockParameters the outbound map of reachable block parameters
     * @param locals the outbound set of discovered local variables
     * @param cleanups the queue of block parameters to clean up after the rest of the sequence is built
     */
    static void buildSequence(Node node, Set<Node> visited, Map<BasicBlock, List<Node>> sequences, Map<BasicBlock, Map<Slot, BlockParameter>> blockParameters, Set<LocalVariableElement> locals, ArrayDeque<BlockParameter> cleanups) {
        if (visited.add(node)) {
            if (node instanceof DebugAddressDeclaration lv) {
                locals.add(lv.getVariable());
            } else if (node instanceof DebugValueDeclaration lv) {
                locals.add(lv.getVariable());
            }
            if (node instanceof OrderedNode on) {
                buildSequence(on.getDependency(), visited, sequences, blockParameters, locals, cleanups);
            }
            if (node instanceof BlockParameter bp) {
                BasicBlock bpBlock = bp.getPinnedBlock();
                blockParameters.computeIfAbsent(bpBlock, Schedule::newMap).put(bp.getSlot(), bp);
                cleanups.addLast(bp);
            }
            int cnt = node.getValueDependencyCount();
            for (int i = 0; i < cnt; i ++) {
                buildSequence(node.getValueDependency(i), visited, sequences, blockParameters, locals, cleanups);
            }
            if (node instanceof Terminator t) {
                cnt = t.getSuccessorCount();
                for (int i = 0; i < cnt; i ++) {
                    buildSequence(t.getSuccessor(i).getTerminator(), visited, sequences, blockParameters, locals, cleanups);
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

    private static void scheduleEarly(BlockInfo root, Map<BasicBlock, BlockInfo> blockInfos, Map<Node, BlockInfo> scheduledNodes, BasicBlock block) {
        Terminator terminator = block.getTerminator();
        if (! scheduledNodes.containsKey(terminator)) {
            scheduleToPinnedBlock(root, blockInfos, scheduledNodes, terminator, block);
            int cnt = terminator.getSuccessorCount();
            for (int i = 0; i < cnt; i ++) {
                scheduleEarly(root, blockInfos, scheduledNodes, terminator.getSuccessor(i));
            }
        }
    }

    private static BlockInfo scheduleDependenciesEarly(BlockInfo root, Map<BasicBlock, BlockInfo> blockInfos, Map<Node, BlockInfo> scheduledNodes, Node node) {
        BlockInfo selected = root;
        int cnt = node.getValueDependencyCount();
        for (int i = 0; i < cnt; i ++) {
            Value valueDependency = node.getValueDependency(i);
            BlockInfo candidate = scheduleEarly(root, blockInfos, scheduledNodes, valueDependency);
            if (candidate.domDepth > selected.domDepth) {
                selected = candidate;
            }
        }
        if (node instanceof OrderedNode) {
            Node dependency = ((OrderedNode) node).getDependency();
            BlockInfo candidate = scheduleEarly(root, blockInfos, scheduledNodes, dependency);
            if (candidate.domDepth > selected.domDepth) {
                selected = candidate;
            }
        }
        return selected;
    }

    private static BlockInfo scheduleEarly(BlockInfo root, Map<BasicBlock, BlockInfo> blockInfos, Map<Node, BlockInfo> scheduledNodes, Node node) {
        assert node != null;
        BlockInfo selected = scheduledNodes.get(node);
        if (selected != null) {
            return selected;
        }
        if (node instanceof PinnedNode) {
            // pinned to a block; always select that block.
            return scheduleToPinnedBlock(root, blockInfos, scheduledNodes, node, ((PinnedNode) node).getPinnedBlock());
        } else if (node instanceof Unschedulable) {
            // always considered available; do not schedule (but do schedule dependencies)
            return scheduleDependenciesEarly(root, blockInfos, scheduledNodes, node);
        } else {
            selected = scheduledNodes.get(node);
            BlockInfo candidate = scheduleDependenciesEarly(root, blockInfos, scheduledNodes, node);
            if (selected == null) {
                selected = candidate;
            }
            // all dependencies have been scheduled
            scheduledNodes.put(node, selected);
            return selected;
        }
    }

    private static BlockInfo scheduleToPinnedBlock(final BlockInfo root, final Map<BasicBlock, BlockInfo> blockInfos, final Map<Node, BlockInfo> scheduledNodes, final Node node, final BasicBlock pinnedBlock) {
        BlockInfo selected = blockInfos.get(pinnedBlock);
        if (selected == null) {
            throw new IllegalStateException("No block selected");
        }
        scheduledNodes.put(node, selected);
        scheduleDependenciesEarly(root, blockInfos, scheduledNodes, node);
        if (node instanceof Terminator t) {
            // schedule all outbound values to blocks; we reduce the set when we build the sequence
            for (Slot slot : t.getOutboundArgumentNames()) {
                scheduleEarly(root, blockInfos, scheduledNodes, t.getOutboundArgument(slot));
            }
        }
        // all dependencies have been scheduled
        return selected;
    }

    static <K, V> Map<K, V> newMap(Object ignored) {
        return new HashMap<>();
    }
}

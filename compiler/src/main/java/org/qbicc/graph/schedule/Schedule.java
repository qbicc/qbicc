package org.qbicc.graph.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.Unschedulable;
import org.qbicc.graph.Node;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.PinnedNode;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import io.smallrye.common.constraint.Assert;

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
     * Get the list of Nodes scheduled to the given basic block.
     *
     * @param block the basic block to look up (must not be {@code null})
     * @return list of nodes which are scheduled to the basic block
     */
    List<Node> getNodesForBlock(BasicBlock block);

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
        Map<Node, BasicBlock> finalMapping = new HashMap<>(scheduledNodes.size());
        Map<BasicBlock, List<Node>> blockToNodesMap = new HashMap<>(allBlocks.length);
        for (Map.Entry<Node, BlockInfo> entry : scheduledNodes.entrySet()) {
            finalMapping.put(entry.getKey(), entry.getValue().block);
            blockToNodesMap.computeIfAbsent(entry.getValue().block, k -> new ArrayList<>()).add(entry.getKey());
        }
        return new Schedule() {
            public BasicBlock getBlockForNode(final Node node) {
                Assert.assertFalse(node instanceof Unschedulable);
                return finalMapping.get(Assert.checkNotNullParam("node", node));
            }
            public List<Node> getNodesForBlock(final BasicBlock block) {
                List<Node> nodes = blockToNodesMap.get(block);
                if (nodes == null) {
                    return List.of();
                }
                return Collections.unmodifiableList(blockToNodesMap.get(block));
            }
        };
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
        if (node.hasValueHandleDependency()) {
            ValueHandle valueHandle = node.getValueHandle();
            BlockInfo candidate = scheduleEarly(root, blockInfos, scheduledNodes, valueHandle);
            if (candidate.domDepth > selected.domDepth) {
                selected = candidate;
            }
        }
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
        if (node instanceof PhiValue) {
            // make sure phi entries were scheduled
            PhiValue phiValue = (PhiValue) node;
            if (phiValue.getPossibleValues().isEmpty()) {
                CompilationContext ctxt = root.block.getTerminator().getElement().getEnclosingType().getContext().getCompilationContext();
                ctxt.error(Location.builder().setNode(node).build(), "Found phi with no possible values");
                return selected;
            }
            for (BasicBlock terminatedBlock : phiValue.getPinnedBlock().getIncoming()) {
                // skip unreachable inputs
                Terminator terminator = terminatedBlock.getTerminator();
                if (blockInfos.containsKey(terminatedBlock)) {
                    Value value = phiValue.getValueForInput(terminator);
                    if (value instanceof PinnedNode && ! blockInfos.containsKey(((PinnedNode) value).getPinnedBlock())) {
                        // the node is reachable even though its block is not!
                        CompilationContext ctxt = root.block.getTerminator().getElement().getEnclosingType().getContext().getCompilationContext();
                        ctxt.error(Location.builder().setNode(node).build(), "Found reachable node in unreachable block");
                        continue;
                    }
                    if (value != null) {
                        scheduleEarly(root, blockInfos, scheduledNodes, value);
                    }
                }
            }
        }
        // all dependencies have been scheduled
        return selected;
    }
}

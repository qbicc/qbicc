package cc.quarkus.qcc.graph.schedule;

import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.PinnedNode;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.Value;
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
     * Create a schedule for the method whose entry block is the given block.
     *
     * @param entryBlock the entry block
     * @return a schedule for the entry block of the method
     */
    static Schedule forMethod(BasicBlock entryBlock) {
        // Simplified algorithm which simply finds *any* valid schedule.
        // todo: Find an optimized schedule.

        int[] indexHolder = new int[] { 1 };
        Map<BasicBlock, BlockInfo> blockInfos = new HashMap<>();
        // 1. First, assign numeric indices for each block
        BlockInfo root = new BlockInfo(entryBlock, 0);
        root.computeIndices(blockInfos, indexHolder);
        final int graphSize = indexHolder[0];
        if (graphSize == 1) {
            // trivial schedule
            return new Schedule() {
                public BasicBlock getBlockForNode(final Node node) {
                    return entryBlock;
                }
            };
        }
        BlockInfo[] allBlocks = new BlockInfo[graphSize];
        // a. Map blocks into the array
        for (BlockInfo value : blockInfos.values()) {
            allBlocks[value.index] = value;
        }
        // 2. Now execute algorithm to get dominators mapping
        new DominatorFinder(graphSize).main(allBlocks);
        // 3. Find the dominator tree depths.
        for (BlockInfo block : allBlocks) {
            block.findDomDepths();
        }

        // now, use the dominator depths to calculate the simplest possible schedule.
        Map<Node, BlockInfo> scheduledNodes = new HashMap<>();
        scheduleEarly(root, blockInfos, scheduledNodes, entryBlock);
        Map<Node, BasicBlock> finalMapping = new HashMap<>(scheduledNodes.size());
        for (Map.Entry<Node, BlockInfo> entry : scheduledNodes.entrySet()) {
            finalMapping.put(entry.getKey(), entry.getValue().block);
        }
        return new Schedule() {
            public BasicBlock getBlockForNode(final Node node) {
                return finalMapping.get(Assert.checkNotNullParam("node", node));
            }
        };
    }

    private static void scheduleEarly(BlockInfo root, Map<BasicBlock, BlockInfo> blockInfos, Map<Node, BlockInfo> scheduledNodes, BasicBlock block) {
        Terminator terminator = block.getTerminator();
        // fix the terminator on its block
        scheduledNodes.put(terminator, blockInfos.get(block));
        scheduleEarly(root, blockInfos, scheduledNodes, terminator);
        int cnt = terminator.getSuccessorCount();
        for (int i = 0; i < cnt; i ++) {
            scheduleEarly(root, blockInfos, scheduledNodes, terminator.getSuccessor(i));
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
        cnt = node.getBasicDependencyCount();
        for (int i = 0; i < cnt; i ++) {
            Node dependency = node.getBasicDependency(i);
            if (dependency != null) {
                BlockInfo candidate = scheduleEarly(root, blockInfos, scheduledNodes, dependency);
                if (candidate.domDepth > selected.domDepth) {
                    selected = candidate;
                }
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
            BasicBlock basicBlock = ((PinnedNode) node).getPinnedBlock();
            selected = blockInfos.get(basicBlock);
            scheduledNodes.put(node, selected);
            scheduleDependenciesEarly(root, blockInfos, scheduledNodes, node);
            if (node instanceof PhiValue) {
                // make sure phi entries were scheduled
                PhiValue phiValue = (PhiValue) node;
                for (BasicBlock block : blockInfos.keySet()) {
                    Value value = phiValue.getValueForBlock(block);
                    if (value != null) {
                        scheduleEarly(root, blockInfos, scheduledNodes, value);
                    }
                }
            }
            // all dependencies have been scheduled
            return selected;
        } else if (node instanceof Literal) {
            // always considered available; do not schedule
            return root;
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
}

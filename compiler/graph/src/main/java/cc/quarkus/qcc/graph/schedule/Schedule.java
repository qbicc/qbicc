package cc.quarkus.qcc.graph.schedule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.ConstantValue;
import cc.quarkus.qcc.graph.MemoryState;
import cc.quarkus.qcc.graph.MemoryStateDependent;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.PinnedNode;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.Value;

/**
 * A linear schedule for basic block instructions.
 */
public interface Schedule {
    /**
     * Get the basic block that this schedule belongs to.
     *
     * @return the basic block
     */
    BasicBlock getBasicBlock();

    /**
     * Get the instruction list for this schedule.  The list will not be empty.  Each node in the list
     * implements {@link Value} or {@link MemoryStateDependent}.  Each {@code Value} in the list appears before any
     * of its usages.
     * <p>
     * The final node in the list implements {@link Terminator}.  Every non-final node in the list which implements
     * {@link MemoryStateDependent} also implements {@link MemoryState}.
     *
     * @return the instruction list (not {@code null})
     */
    List<Node> getInstructions();

    /**
     * Get the schedule for the given successor block.  The block must be a direct successor to this schedule's block.
     *
     * @return the schedule
     * @throws IllegalArgumentException if the block is not a successor to this schedule's block
     */
    Schedule getSuccessorSchedule(BasicBlock successor) throws IllegalArgumentException;

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
        return root.createSchedules(blockInfos);
    }

    private static void scheduleEarly(BlockInfo root, Map<BasicBlock, BlockInfo> blockInfos, Map<Node, BlockInfo> scheduledNodes, BasicBlock block) {
        Terminator terminator = block.getTerminator();
        // fix the terminator on its block
        scheduledNodes.put(terminator, blockInfos.get(block));
        scheduleEarly(root, blockInfos, scheduledNodes, terminator);
        int cnt = block.getSuccessorCount();
        for (int i = 0; i < cnt; i ++) {
            scheduleEarly(root, blockInfos, scheduledNodes, block.getSuccessor(i));
        }
    }

    private static BlockInfo scheduleEarly(BlockInfo root, Map<BasicBlock, BlockInfo> blockInfos, Map<Node, BlockInfo> scheduledNodes, Node node) {
        BlockInfo selected;
        if (node instanceof PinnedNode) {
            // pinned to a block; always select that block.
            selected = blockInfos.get(((PinnedNode) node).getBasicBlock());
        } else if (node instanceof ConstantValue) {
            // always considered available; do not schedule
            return root;
        } else {
            selected = scheduledNodes.get(node);
            if (selected == null) {
                selected = root;
                int cnt = node.getValueDependencyCount();
                for (int i = 0; i < cnt; i ++) {
                    BlockInfo candidate = scheduleEarly(root, blockInfos, scheduledNodes, node.getValueDependency(i));
                    if (candidate.domDepth > selected.domDepth) {
                        selected = candidate;
                    }
                }
                if (node instanceof MemoryStateDependent) {
                    MemoryState dependency = ((MemoryStateDependent) node).getMemoryDependency();
                    if (dependency != null) {
                        BlockInfo candidate = scheduleEarly(root, blockInfos, scheduledNodes, dependency);
                        if (candidate.domDepth > selected.domDepth) {
                            selected = candidate;
                        }
                    }
                }
            }
        }
        // all dependencies have been scheduled
        selected.scheduledInstructions.add(node);
        return selected;
    }
}

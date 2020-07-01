package cc.quarkus.qcc.graph.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.Node;

final class BlockInfo {
    final BasicBlock block;
    final int index;
    final ArrayList<Node> scheduledInstructions = new ArrayList<>();
    Schedule schedule;
    BlockInfo dominator;
    int domDepth = -1;

    BlockInfo(final BasicBlock block, final int index) {
        this.block = block;
        this.index = index;
    }

    void computeIndices(final Map<BasicBlock, BlockInfo> blockInfos, int[] holder) {
        blockInfos.put(block, this);
        int cnt = block.getSuccessorCount();
        for (int i = 0; i < cnt; i ++) {
            processBlock(blockInfos, holder, block.getSuccessor(i));
        }
    }

    private void processBlock(final Map<BasicBlock, BlockInfo> blockInfos, int[] holder, BasicBlock block) {
        if (! blockInfos.containsKey(block)) {
            new BlockInfo(block, holder[0]++).computeIndices(blockInfos, holder);
        }
    }

    int findDomDepths() {
        int domDepth = this.domDepth;
        if (domDepth == -1) {
            domDepth = this.domDepth = dominator == null ? 0 : dominator.findDomDepths() + 1;
        }
        return domDepth;
    }

    Schedule createSchedules(Map<BasicBlock, BlockInfo> blockInfos) {
        Schedule schedule = this.schedule;
        if (schedule == null) {
            // todo: this is too heavy
            Map<BasicBlock, Schedule> successors = new HashMap<>();
            BasicBlock block = this.block;
            List<Node> scheduledInstructions = Collections.unmodifiableList(this.scheduledInstructions);
            schedule = this.schedule = new Schedule() {
                public BasicBlock getBasicBlock() {
                    return block;
                }

                public List<Node> getInstructions() {
                    return scheduledInstructions;
                }

                public Schedule getSuccessorSchedule(final BasicBlock successor) throws IllegalArgumentException {
                    Schedule successorSchedule = successors.get(successor);
                    if (successorSchedule == null) {
                        throw new IllegalArgumentException("Block " + successor + " is not a successor of " + block);
                    }
                    return successorSchedule;
                }
            };
            int cnt = block.getSuccessorCount();
            for (int i = 0; i < cnt; i ++) {
                BasicBlock successor = block.getSuccessor(i);
                successors.put(successor, blockInfos.get(successor).createSchedules(blockInfos));
            }
        }
        return schedule;
    }
}

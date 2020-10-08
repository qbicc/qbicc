package cc.quarkus.qcc.graph.schedule;

import java.util.Map;

import cc.quarkus.qcc.graph.BasicBlock;
import io.smallrye.common.constraint.Assert;

final class BlockInfo {
    final BasicBlock block;
    final int index;
    BlockInfo dominator;
    int domDepth = -1;

    BlockInfo(final BasicBlock block, final int index) {
        this.block = Assert.checkNotNullParam("block", block);
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
}

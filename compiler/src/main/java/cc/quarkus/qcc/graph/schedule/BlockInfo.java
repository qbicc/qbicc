package cc.quarkus.qcc.graph.schedule;

import java.util.BitSet;
import java.util.Map;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.Terminator;
import io.smallrye.common.constraint.Assert;

final class BlockInfo {
    final BasicBlock block;
    final int index;
    BlockInfo dominator;
    int domDepth = -1;

    // dominator finder fields
    final BitSet pred = new BitSet();
    final BitSet succ = new BitSet();
    final BitSet bucket = new BitSet();
    int parent;
    int ancestor;
    int child;
    int vertex;
    int label;
    int semi;
    int size;

    BlockInfo(final BasicBlock block, final int index) {
        this.block = Assert.checkNotNullParam("block", block);
        this.index = index;
    }

    void computeIndices(final Map<BasicBlock, BlockInfo> blockInfos, int[] holder) {
        blockInfos.put(block, this);
        Terminator terminator = block.getTerminator();
        int cnt = terminator.getSuccessorCount();
        for (int i = 0; i < cnt; i ++) {
            processBlock(blockInfos, holder, terminator.getSuccessor(i));
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

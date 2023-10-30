package org.qbicc.graph.schedule;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;

import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.Terminator;
import io.smallrye.common.constraint.Assert;
import org.qbicc.graph.Value;

final class BlockInfo {
    final BasicBlock block;
    final int index;
    int dominator;
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
    final HashSet<Value> liveIn = new HashSet<>();
    final HashSet<Value> liveOut = new HashSet<>();

    BlockInfo(final BasicBlock block, final int index) {
        this.block = Assert.checkNotNullParam("block", block);
        this.index = index;
    }

    void computeIndices(final Map<BasicBlock, BlockInfo> blockInfos, int[] holder) {
        blockInfos.put(block, this);
        Terminator terminator = block.getTerminator();
        int cnt = terminator.getSuccessorCount();
        for (int i = 0; i < cnt; i ++) {
            BasicBlock block = terminator.getSuccessor(i);
            processBlock(blockInfos, holder, block);
            succ.set(blockInfos.get(block).index - 1);
        }
    }

    private void processBlock(final Map<BasicBlock, BlockInfo> blockInfos, int[] holder, BasicBlock block) {
        if (! blockInfos.containsKey(block)) {
            new BlockInfo(block, holder[0]++).computeIndices(blockInfos, holder);
        }
    }

    int findDomDepths(final BlockInfo[] infos) {
        int domDepth = this.domDepth;
        if (domDepth == -1) {
            domDepth = this.domDepth = dominator == 0 ? 0 : infos[dominator - 1].findDomDepths(infos) + 1;
        }
        return domDepth;
    }

    boolean dominates(final BlockInfo[] allBlocks, final BlockInfo other) {
        // this block dominates other if it is the immediate dominator of other or its ancestor
        if (this == other || index == 1) {
            // blocks dominate themselves, and block 1 dominates everything
            return true;
        }
        if (other.index == 1) {
            // we're not block 1 but the other is, so we cannot possibly dominate it
            return false;
        }
        return dominates(allBlocks, allBlocks[other.dominator - 1]); // indexes are 1-based, array is 0-based
    }

    BitSet dominateSet(final BlockInfo[] allBlocks) {
        BitSet set = new BitSet(allBlocks.length);
        for (int i = 0; i < allBlocks.length; i++) {
            final BlockInfo bi = allBlocks[i];
            if (dominates(allBlocks, bi)) {
                set.set(i);
            }
        }
        return set;
    }
}

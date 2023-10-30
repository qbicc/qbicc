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
    final BitSet dom = new BitSet();
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

    static void computeDomSets(final BlockInfo[] allBlocks) {
        for (BlockInfo block : allBlocks) {
            // blocks dominate themselves, and block 1 dominates everything
            if (block.index == 1) {
                block.dom.set(1, allBlocks.length + 1);
            } else {
                block.dom.set(block.index);
                // recursively add ourselves to our dominators up the tree
                addBit(allBlocks, block.index, allBlocks[block.dominator - 1]);
            }
        }
    }

    private static void addBit(final BlockInfo[] allBlocks, int dominatedIndex, BlockInfo dominator) {
        if (! dominator.dom.get(dominatedIndex)) {
            dominator.dom.set(dominatedIndex);
            addBit(allBlocks, dominatedIndex, allBlocks[dominator.dominator - 1]);
        }
    }

    boolean dominates(final BlockInfo other) {
        return dom.get(other.index);
    }

    BitSet dominateSet() {
        return dom;
    }
}

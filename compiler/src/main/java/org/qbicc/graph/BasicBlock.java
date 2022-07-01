package org.qbicc.graph;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class BasicBlock {
    private final BlockEntry blockEntry;
    private final Terminator terminator;
    private BlockLabel myLabel;
    private boolean reachable;
    private Set<BasicBlock> incoming = Set.of();
    private Set<Loop> loops = Set.of();
    private int index;

    BasicBlock(final BlockEntry blockEntry, final Terminator terminator) {
        this.blockEntry = blockEntry;
        this.terminator = terminator;
    }

    public BlockEntry getBlockEntry() {
        return blockEntry;
    }

    public Terminator getTerminator() {
        return terminator;
    }

    // nodes contain links to handles, not to nodes
    BlockLabel getHandle() {
        BlockLabel myHandle = this.myLabel;
        if (myHandle == null) {
            myHandle = this.myLabel = new BlockLabel();
            myHandle.setTarget(this);
        }
        return myHandle;
    }

    BlockLabel getHandleIfExists() {
        return myLabel;
    }

    void setHandle(BlockLabel newHandle) {
        assert myLabel == null;
        myLabel = newHandle;
    }

    /**
     * Get the set of loops that this block is a part of.
     *
     * @return the set of loops that this block is a part of
     */
    public Set<Loop> getLoops() {
        return loops;
    }

    void setLoops(Set<Loop> loops) {
        this.loops = loops;
    }

    public boolean isReachable() {
        return reachable;
    }

    public boolean setReachableFrom(BasicBlock from) {
        if (from != null && ! incoming.contains(from)) {
            if (incoming.isEmpty()) {
                incoming = Set.of(from);
            } else if (incoming.size() == 1) {
                incoming = Set.of(from, incoming.iterator().next());
            } else if (incoming.size() == 2) {
                Set<BasicBlock> old = this.incoming;
                incoming = new LinkedHashSet<>();
                incoming.addAll(old);
                incoming.add(from);
            } else {
                incoming.add(from);
            }
        }
        boolean old = this.reachable;
        if (old) {
            return false;
        }
        this.reachable = true;
        return true;
    }

    public Set<BasicBlock> getIncoming() {
        return incoming;
    }

    public boolean isSucceededBy(final BasicBlock block) {
        int cnt = terminator.getSuccessorCount();
        for (int i = 0; i < cnt; i ++) {
            if (block.equals(terminator.getSuccessor(i))) {
                return true;
            }
        }
        return false;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static final class Loop {
        private final BasicBlock startBlock;
        private final BasicBlock endBlock;

        public Loop(BasicBlock startBlock, BasicBlock endBlock) {
            this.startBlock = Assert.checkNotNullParam("startBlock", startBlock);
            this.endBlock = Assert.checkNotNullParam("endBlock", endBlock);
        }

        public BasicBlock getStartBlock() {
            return startBlock;
        }

        public BasicBlock getEndBlock() {
            return endBlock;
        }

        @Override
        public int hashCode() {
            return Objects.hash(startBlock, endBlock);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Loop && equals((Loop) obj);
        }

        public boolean equals(Loop obj) {
            return this == obj || obj != null && startBlock == obj.startBlock && endBlock == obj.endBlock;
        }
    }
}

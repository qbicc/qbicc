package org.qbicc.graph;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    // set by scheduler
    private Set<BasicBlock> incoming = Set.of();
    private Set<Loop> loops = Set.of();
    private int index;
    private Set<Value> liveOuts;
    private List<Node> instructions;
    private Map<Slot, BlockParameter> usedParameters;

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
     * Get the sequence of instructions for this block.
     *
     * @return the instruction list (not {@code null})
     */
    public List<Node> getInstructions() {
        List<Node> instructions = this.instructions;
        if (instructions == null) {
            throw new IllegalStateException("Scheduling is not yet complete");
        }
        return instructions;
    }

    public void setInstructions(List<Node> instructions) {
        // only call from scheduler...
        this.instructions = instructions;
    }

    /**
     * Get the block parameter for the given slot, if any.
     *
     * @param slot the slot (must not be {@code null})
     * @return the block parameter, or {@code null} if there is none for {@code slot} or there is but it was never used
     */
    public BlockParameter getBlockParameter(Slot slot) {
        Map<Slot, BlockParameter> usedParameters = this.usedParameters;
        if (usedParameters == null) {
            throw new IllegalStateException("Scheduling is not yet complete");
        }
        return usedParameters.get(slot);
    }

    public Set<Slot> getUsedParameterSlots() {
        Map<Slot, BlockParameter> usedParameters = this.usedParameters;
        if (usedParameters == null) {
            throw new IllegalStateException("Scheduling is not yet complete");
        }
        return usedParameters.keySet();
    }

    public void setUsedParameters(Map<Slot, BlockParameter> usedParameters) {
        // only call from scheduler...
        this.usedParameters = usedParameters;
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

    /**
     * Get the set of reference-typed values that must be alive when this block exits.
     *
     * @return the (possibly empty) set of live values
     */
    public Set<Value> getLiveOuts() {
        return liveOuts;
    }

    /**
     * For use by the scheduler.
     *
     * @param liveOuts the live outs to add
     */
    public void setLiveOuts(Set<Value> liveOuts) {
        this.liveOuts = liveOuts;
    }

    public StringBuilder toString(StringBuilder b) {
        return b.append("bb").append(index);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
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

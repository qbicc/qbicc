package org.qbicc.graph;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.ReferenceType;

/**
 *
 */
public final class BasicBlock {
    private static final VarHandle locallyUsedRefsHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "locallyUsedRefs", VarHandle.class, BasicBlock.class, Set.class);

    private final BlockEntry blockEntry;
    private final Terminator terminator;
    private BlockLabel myLabel;
    private boolean reachable;
    // set by scheduler
    private Set<BasicBlock> incoming = Set.of();
    private Set<Loop> loops = Set.of();
    private int index;
    @SuppressWarnings("unused") // localLiveInsHandle
    private volatile Set<Value> locallyUsedRefs;
    private volatile Set<Value> liveOuts;
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

    /*
       Algorithm explanation:

       Calculating locally-used values only considers values which are local to the block.
       Naively, one might consider using the double-checked locking idiom to avoid the
       redundant calculation; however, trouble begins when considering the live-out set, which
       must visit all successor blocks in a recursive manner. Because blocks can loop,
       if the locally-used computation used locks, the algorithm would expose a risk of deadlock.

       Since the local-use calculation is fairly simple, the risk of duplicate computation seems less than
       the risk of deadlock.
     */

    /**
     * Get the set of reference-typed values that are used locally within this block.
     *
     * @return the (possibly empty) set of locally-used values
     */
    public Set<Value> getLocallyUsedReferenceValues() {
        Set<Value> locallyUsedRefs = this.locallyUsedRefs;
        if (locallyUsedRefs == null) {
            HashSet<Value> live = new HashSet<>();
            for (Node node : getInstructions()) {
                addLocallyUsedValues(node, live);
            }
            locallyUsedRefs = Set.copyOf(live);
            @SuppressWarnings("unchecked")
            final Set<Value> appearing = (Set<Value>) locallyUsedRefsHandle.compareAndExchange(this, null, locallyUsedRefs);
            if (appearing != null) {
                locallyUsedRefs = appearing;
            }
        }
        return locallyUsedRefs;
    }

    private void addLocallyUsedValues(final Node node, final HashSet<Value> live) {
        final int cnt = node.getValueDependencyCount();
        for (int i = 0; i < cnt; i ++) {
            final Value value = node.getValueDependency(i);
            if (value.getType() instanceof ReferenceType) {
                live.add(value);
            }
        }
    }

    /**
     * Get the set of reference-typed values that must be alive when this block exits.
     *
     * @return the (possibly empty) set of live values
     */
    public Set<Value> getLiveOuts() {
        Set<Value> liveOuts = this.liveOuts;
        if (liveOuts == null) {
            synchronized (this) {
                liveOuts = this.liveOuts;
                if (liveOuts == null) {
                    final Terminator t = getTerminator();
                    final int cnt = t.getSuccessorCount();
                    if (cnt == 0) {
                        this.liveOuts = liveOuts = Set.of();
                    } else {
                        this.liveOuts = liveOuts = Set.copyOf(visitForLiveOuts(this, new HashSet<>(), new HashSet<>()));
                    }
                }
            }
        }
        return liveOuts;
    }

    private HashSet<Value> visitForLiveOuts(BasicBlock block, HashSet<BasicBlock> visited, HashSet<Value> live) {
        final Terminator t = block.getTerminator();
        final int cnt = t.getSuccessorCount();
        for (int i = 0; i < cnt; i ++) {
            final BasicBlock successor = t.getSuccessor(i);
            if (visited.add(successor)){
                live.addAll(successor.getLocallyUsedReferenceValues());
                visitForLiveOuts(successor, visited, live);
            }
        }
        return live;
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

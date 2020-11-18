package cc.quarkus.qcc.graph;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class BasicBlock {
    private final BlockEntry blockEntry;
    private final Terminator terminator;
    private BlockLabel myLabel;
    // used by phi nodes
    Map<PhiValue.Key, Value> outboundValues = Map.of();

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

    public Set<BasicBlock> calculateReachableBlocks() {
        Set<BasicBlock> set = new LinkedHashSet<>();
        findReachable(set);
        return set;
    }

    private void findReachable(final Set<BasicBlock> set) {
        if (set.add(this)) {
            Terminator ti = terminator;
            int cnt = ti.getSuccessorCount();
            for (int i = 0; i < cnt; i ++) {
                ti.getSuccessor(i).findReachable(set);
            }
        }
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
}

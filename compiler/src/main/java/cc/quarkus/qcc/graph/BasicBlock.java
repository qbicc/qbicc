package cc.quarkus.qcc.graph;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public final class BasicBlock {
    private final BlockEntry blockEntry;
    private final Terminator terminator;
    private BlockLabel myLabel;
    private boolean reachable;
    private Set<BasicBlock> incoming = Set.of();

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

    public boolean isReachable() {
        return reachable;
    }

    public boolean setReachableFrom(BasicBlock from) {
        if (from != null) {
            if (incoming.isEmpty()) {
                incoming = Set.of(from);
            } else if (incoming.size() == 1) {
                incoming = Set.of(from, incoming.iterator().next());
            } else if (incoming.size() == 2) {
                Set<BasicBlock> old = this.incoming;
                incoming = new HashSet<>();
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
}

package org.qbicc.graph;

/**
 * A node which is pinned to a corresponding basic block and cannot exist outside of it.
 */
public interface PinnedNode extends Node {
    default BasicBlock getPinnedBlock() {
        return BlockLabel.requireTargetOf(getPinnedBlockLabel());
    }

    BlockLabel getPinnedBlockLabel();
}

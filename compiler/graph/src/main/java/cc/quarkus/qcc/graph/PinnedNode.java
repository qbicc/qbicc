package cc.quarkus.qcc.graph;

/**
 * A node which is pinned to a corresponding basic block and cannot exist outside of it.
 */
public interface PinnedNode extends Node {
    BasicBlock getBasicBlock();
}

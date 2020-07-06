package cc.quarkus.qcc.graph;

/**
 *
 */
public interface PhiMemoryState extends PinnedNode, MemoryState {
    MemoryState getMemoryStateForBlock(BasicBlock input);
    void setMemoryStateForBlock(BasicBlock input, MemoryState memoryState);
}

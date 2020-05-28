package cc.quarkus.qcc.graph2;

/**
 * A node which represents the program memory state.  Initially the memory state is a partial view
 * over both program order and happens-before order.  Optimization stages may perform some allowed reordering
 * before the backend emits the final program.
 */
public interface MemoryState extends ProgramNode {
    MemoryState getMemoryDependency();
    void setMemoryDependency(MemoryState memoryState);
}

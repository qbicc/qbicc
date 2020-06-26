package cc.quarkus.qcc.graph;

/**
 *
 */
public interface MemoryStateDependent extends Node {
    MemoryState getMemoryDependency();

    void setMemoryDependency(MemoryState memoryState);
}

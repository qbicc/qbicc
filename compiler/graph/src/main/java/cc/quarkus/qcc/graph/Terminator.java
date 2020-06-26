package cc.quarkus.qcc.graph;

/**
 * A construct which terminates a block.  It holds a dependency on the preceding sequence of inter-thread actions.
 */
public interface Terminator extends MemoryStateDependent {
}

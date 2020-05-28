package cc.quarkus.qcc.graph2;

/**
 * A construct which terminates a block.  It holds a dependency on the preceding sequence of inter-thread actions.
 */
public interface Terminator extends MemoryState {
}

package cc.quarkus.qcc.graph;

/**
 * A node which may be triable if an exception handling context is active.
 */
public interface Triable extends Node {
    <T, R> R accept(final TriableVisitor<T, R> visitor, final T param);
}

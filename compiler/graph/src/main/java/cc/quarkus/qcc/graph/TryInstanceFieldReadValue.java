package cc.quarkus.qcc.graph;

/**
 * An instance field read instruction of a possibly-{@code null} instance that occurs within a {@code try} block.
 */
public interface TryInstanceFieldReadValue extends Try, Goto, InstanceFieldReadValue {
}

package cc.quarkus.qcc.graph2;

/**
 * An instance field read instruction of a possibly-{@code null} instance that occurs within a {@code try} block.
 */
public interface TryInstanceFieldReadValue extends Try, Goto, InstanceFieldReadValue {
}

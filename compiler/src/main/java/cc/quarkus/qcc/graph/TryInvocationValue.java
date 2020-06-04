package cc.quarkus.qcc.graph;

/**
 * An invoke instruction within a {@code try} block which returns a value.
 */
public interface TryInvocationValue extends Try, InvocationValue, Goto {
}

package cc.quarkus.qcc.graph;

/**
 * An invocation within a try block which may throw an exception.
 */
public interface TryInvocation extends Try, Invocation, Goto {
}

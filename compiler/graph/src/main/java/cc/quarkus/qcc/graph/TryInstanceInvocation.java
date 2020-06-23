package cc.quarkus.qcc.graph;

/**
 * An invocation within a try block which may throw an exception (possibly a {@code NullPointerException} if
 * it is an instance invocation and the receiver is {@code null}).
 */
public interface TryInstanceInvocation extends TryInvocation, InstanceInvocation {
}

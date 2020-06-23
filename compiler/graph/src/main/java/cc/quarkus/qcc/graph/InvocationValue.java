package cc.quarkus.qcc.graph;

/**
 * An invoke instruction which returns a value.
 */
public interface InvocationValue extends Invocation, Value {
    default Type getType() {
        return getInvocationTarget().getReturnType();
    }
}

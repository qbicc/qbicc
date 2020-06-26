package cc.quarkus.qcc.graph;

/**
 * An invoke instruction which returns a value.
 */
public interface InvocationValue extends Invocation, Value, GraphFactory.MemoryStateValue {
    default Type getType() {
        return getInvocationTarget().getReturnType();
    }

    default Value getValue() {
        return this;
    }

    default MemoryState getMemoryState() {
        return this;
    }
}

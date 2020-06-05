package cc.quarkus.qcc.graph;

/**
 * An invoke instruction which returns a value.
 */
public interface InvocationValue extends Invocation, Value {
    default Type getType() {
        // TODO: get type from return type of method descriptor
        return Type.S32;
    }
}

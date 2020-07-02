package cc.quarkus.qcc.graph;

/**
 * The length of a Java array instance.
 */
public interface ArrayLengthValue extends InstanceOperation, Value {
    default Type getType() {
        return Type.S32;
    }
}

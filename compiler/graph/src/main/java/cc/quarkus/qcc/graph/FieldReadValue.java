package cc.quarkus.qcc.graph;

/**
 * A field read value.
 */
public interface FieldReadValue extends Value, FieldOperation {
    default Type getType() {
        // TODO: get type from field descriptor
        return Type.S32;
    }
}

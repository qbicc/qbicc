package cc.quarkus.qcc.graph;

/**
 * A field read value.
 */
public interface FieldReadValue extends Value, FieldOperation {
    default Type getType() {
        return getFieldOwner().getDefinition().resolve().findField(getFieldName()).getType();
    }
}

package cc.quarkus.qcc.graph;

/**
 * A field read value.
 */
public interface FieldReadValue extends Value, FieldOperation, GraphFactory.MemoryStateValue {
    default Type getType() {
        return getFieldOwner().getDefinition().resolve().findField(getFieldName()).getType();
    }

    default Value getValue() {
        return this;
    }

    default MemoryState getMemoryState() {
        return this;
    }
}

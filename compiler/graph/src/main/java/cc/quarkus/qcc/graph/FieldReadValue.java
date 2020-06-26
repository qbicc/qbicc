package cc.quarkus.qcc.graph;

/**
 * A field read value.
 */
public interface FieldReadValue extends Value, FieldOperation, GraphFactory.MemoryStateValue {
    default Type getType() {
        // TODO: get type from field descriptor
        return Type.S32;
    }

    default Value getValue() {
        return this;
    }

    default MemoryState getMemoryState() {
        return this;
    }
}

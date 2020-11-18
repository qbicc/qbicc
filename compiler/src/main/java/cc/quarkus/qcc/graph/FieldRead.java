package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ValueType;

/**
 * A field read value.
 */
public interface FieldRead extends Value, FieldOperation {
    default ValueType getType() {
        return getFieldElement().getType();
    }
}

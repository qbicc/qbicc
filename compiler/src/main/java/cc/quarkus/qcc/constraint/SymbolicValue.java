package cc.quarkus.qcc.constraint;

import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.ValueType;

public interface SymbolicValue extends Value {
    default ValueType getType() {
        return null;
    }
}

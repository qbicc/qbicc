package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.TypeIdType;
import cc.quarkus.qcc.type.ValueType;

/**
 * The type ID of a given value.
 */
public final class TypeIdOf extends AbstractValue {
    private final TypeIdType type;
    private final Value value;

    TypeIdOf(final TypeIdType type, final Value value) {
        this.type = type;
        this.value = value;
    }

    public ValueType getType() {
        return type;
    }

    public Value getValue() {
        return value;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

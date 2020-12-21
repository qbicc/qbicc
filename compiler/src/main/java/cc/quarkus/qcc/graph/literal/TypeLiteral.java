package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.TypeType;
import cc.quarkus.qcc.type.ValueType;

/**
 * A constant value whose type is a {@link TypeType} and whose value is a {@link ValueType}.
 */
public final class TypeLiteral extends Literal {

    private final ValueType value;

    TypeLiteral(final ValueType value) {
        this.value = value;
    }

    public TypeType getType() {
        return value.getTypeType();
    }

    public boolean equals(final Literal other) {
        // every instance is unique
        return this == other;
    }

    public int hashCode() {
        // every instance is unique
        return System.identityHashCode(this);
    }

    public String toString() {
        return value.toString();
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

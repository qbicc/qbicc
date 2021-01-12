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
        return other instanceof TypeLiteral && equals((TypeLiteral) other);
    }

    public boolean equals(final TypeLiteral other) {
        return this == other || other != null && value.equals(other.value);
    }

    public int hashCode() {
        return value.hashCode();
    }

    public String toString() {
        return value.toString();
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

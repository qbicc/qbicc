package org.qbicc.graph.literal;

import org.qbicc.type.TypeIdType;
import org.qbicc.type.ValueType;

/**
 * A constant value whose type is a {@link TypeIdType} and whose value is a {@link ValueType}.
 */
public final class TypeLiteral extends Literal {

    private final ValueType value;

    TypeLiteral(final ValueType value) {
        this.value = value;
    }

    public TypeIdType getType() {
        return value.getTypeType();
    }

    public ValueType getValue() {
        return value;
    }

    public boolean isZero() {
        return false;
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

    @Override
    public StringBuilder toString(StringBuilder b) {
        return value.toString(b);
    }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

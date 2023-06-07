package org.qbicc.graph.literal;

import org.qbicc.graph.Value;
import org.qbicc.type.BooleanType;

public final class BooleanLiteral extends WordLiteral {
    private final BooleanType type;
    private final boolean value;

    BooleanLiteral(final BooleanType type, final boolean value) {
        this.type = type;
        this.value = value;
    }

    public BooleanType getType() {
        return type;
    }

    public boolean booleanValue() {
        return value;
    }

    public boolean isZero() {
        return !value;
    }

    public boolean equals(final Literal other) {
        return other instanceof BooleanLiteral && equals((BooleanLiteral) other);
    }

    public boolean equals(final BooleanLiteral other) {
        return this == other || other != null && value == other.value && type.equals(other.type);
    }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public int hashCode() {
        return Boolean.hashCode(value);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append(value);
    }

    @Override
    public boolean isDefEq(Value other) {
        return equals(other);
    }

    @Override
    public boolean isDefNe(Value other) {
        return other instanceof BooleanLiteral && ! equals((BooleanLiteral) other);
    }
}

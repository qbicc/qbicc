package org.qbicc.graph.literal;

import org.qbicc.type.WordType;

public class BitCastLiteral extends Literal {
    final Literal value;
    final WordType toType;

    BitCastLiteral(final Literal value, final WordType toType) {
        this.value = value;
        this.toType = toType;
    }

    public WordType getType() {
        return toType;
    }

    public Literal getValue() { return value; }

    public boolean isZero() {
        return value.isZero();
    }

    public boolean equals(final Literal other) {
        return other instanceof BitCastLiteral && equals((BitCastLiteral) other);
    }

    public boolean equals(final BitCastLiteral other) {
        return other == this || other != null && toType.equals(other.toType) && value.equals(other.value);
    }

    @Override
    Literal bitCast(LiteralFactory lf, WordType toType) {
        return lf.bitcastLiteral(value, toType);
    }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public int hashCode() { return value.hashCode() * 19 + toType.hashCode(); }

    @Override
    public boolean isNullable() {
        return value.isNullable();
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append("bitcast").append('(');
        value.toString(b);
        b.append(" to ");
        toType.toString(b);
        b.append(')');
        return b;
    }
}

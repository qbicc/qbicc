package org.qbicc.graph.literal;

import org.qbicc.graph.ValueVisitor;
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

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public int hashCode() { return value.hashCode() * 19 + toType.hashCode(); }

    public String toString() {
        return "bitcast ("+value+" to "+toType+")";
    }
}

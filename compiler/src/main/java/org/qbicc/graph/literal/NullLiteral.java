package org.qbicc.graph.literal;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.NullableType;

/**
 * A literal of the {@code null} value of pointers and references.
 */
public final class NullLiteral extends Literal {
    private final NullableType type;

    NullLiteral(final NullableType type) {
        this.type = type;
    }

    public NullableType getType() {
        return type;
    }

    @Override
    public boolean isZero() {
        return true;
    }

    public boolean equals(final Literal other) {
        return other instanceof NullLiteral && equals((NullLiteral) other);
    }

    public boolean equals(final NullLiteral other) {
        return other == this || other != null && type.equals(other.type);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public int hashCode() {
        return NullLiteral.class.hashCode();
    }

    public String toString() {
        return "null";
    }
}

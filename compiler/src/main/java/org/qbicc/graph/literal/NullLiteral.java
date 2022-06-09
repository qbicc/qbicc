package org.qbicc.graph.literal;

import org.qbicc.graph.Value;
import org.qbicc.graph.ValueVisitor;
import org.qbicc.graph.ValueVisitorLong;
import org.qbicc.type.IntegerType;
import org.qbicc.type.NullableType;
import org.qbicc.type.WordType;

/**
 * A literal of the {@code null} value of pointers and references.
 */
public final class NullLiteral extends WordLiteral {
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

    @Override
    Literal bitCast(LiteralFactory lf, WordType toType) {
        if (toType instanceof IntegerType) {
            return lf.literalOf((IntegerType) toType, 0);
        } else if (toType instanceof NullableType) {
            return lf.nullLiteralOfType((NullableType) toType);
        }
        return super.bitCast(lf, toType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean isDefNe(Value other) {
        return ! other.isNullable();
    }

    @Override
    public boolean isDefEq(Value other) {
        return other instanceof NullLiteral;
    }

    public int hashCode() {
        return NullLiteral.class.hashCode();
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append("null");
    }

    public String toString() {
        return "null";
    }
}

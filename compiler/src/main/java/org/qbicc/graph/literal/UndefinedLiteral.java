package org.qbicc.graph.literal;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;

/**
 * The undefined value.  Usage of an undefined value results in a compilation error.
 */
public final class UndefinedLiteral extends Literal {
    private final ValueType type;

    UndefinedLiteral(final ValueType type) {
        this.type = type;
    }

    public boolean isZero() {
        return false;
    }

    public boolean equals(final Literal other) {
        return other instanceof UndefinedLiteral;
    }

    public ValueType getType() {
        return type;
    }

    @Override
    Literal bitCast(LiteralFactory lf, WordType toType) {
        return lf.undefinedLiteralOfType(toType);
    }

    public int hashCode() {
        return type.hashCode();
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append("undef");
    }
}

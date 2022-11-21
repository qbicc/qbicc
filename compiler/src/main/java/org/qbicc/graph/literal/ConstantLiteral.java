package org.qbicc.graph.literal;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;

/**
 * A constant literal.  Constant literals are resolved to values during the analysis phase.
 */
public final class ConstantLiteral extends Literal {
    private final ValueType type;

    ConstantLiteral(final ValueType type) {
        this.type = type;
    }

    public boolean isZero() {
        return false;
    }

    public boolean equals(final Literal other) {
        return other instanceof ConstantLiteral;
    }

    public ValueType getType() {
        return type;
    }

    @Override
    Literal bitCast(LiteralFactory lf, WordType toType) {
        return lf.constantLiteralOfType(toType);
    }

    public int hashCode() {
        return type.hashCode();
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append("constant");
    }
}

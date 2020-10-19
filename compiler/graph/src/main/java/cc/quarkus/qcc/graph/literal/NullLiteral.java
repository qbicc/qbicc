package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.constraint.Constraint;
import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.NullType;
import cc.quarkus.qcc.type.ValueType;

/**
 * A {@code null} value of some type.
 */
public final class NullLiteral extends Literal {
    private final NullType type;

    NullLiteral(final NullType type) {
        this.type = type;
    }

    public ValueType getType() {
        return type;
    }

    public Constraint getConstraint() {
        return Constraint.equalTo(this);
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

    public String toString() {
        return "null";
    }
}

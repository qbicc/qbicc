package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.constraint.Constraint;
import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.ReferenceType;

/**
 * The literal representing the current thread.
 */
public final class CurrentThreadLiteral extends Literal {
    private final ReferenceType type;

    CurrentThreadLiteral(final ReferenceType type) {
        this.type = type;
    }

    public ReferenceType getType() {
        return type;
    }

    public Constraint getConstraint() {
        return Constraint.equalTo(this);
    }

    public boolean equals(final Literal other) {
        return other instanceof CurrentThreadLiteral && equals((CurrentThreadLiteral) other);
    }

    public boolean equals(final CurrentThreadLiteral other) {
        return other == this || other != null && type.equals(other.type);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public int hashCode() {
        return CurrentThreadLiteral.class.hashCode();
    }

    public String toString() {
        return "currentThread";
    }
}

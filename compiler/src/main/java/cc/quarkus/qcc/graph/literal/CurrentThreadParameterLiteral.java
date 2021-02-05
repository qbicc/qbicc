package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.ReferenceType;

/**
 * The literal representing the current thread when it is used as a parameter.
 */
public final class CurrentThreadParameterLiteral extends Literal {
    private final ReferenceType type;

    CurrentThreadParameterLiteral(final ReferenceType type) {
        this.type = type;
    }

    public ReferenceType getType() {
        return type;
    }

    public boolean equals(final Literal other) {
        return other instanceof CurrentThreadParameterLiteral && equals((CurrentThreadParameterLiteral) other);
    }

    public boolean equals(final CurrentThreadParameterLiteral other) {
        return other == this || other != null && type.equals(other.type);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public int hashCode() {
        return CurrentThreadParameterLiteral.class.hashCode();
    }

    public String toString() {
        return "currentThread";
    }
}

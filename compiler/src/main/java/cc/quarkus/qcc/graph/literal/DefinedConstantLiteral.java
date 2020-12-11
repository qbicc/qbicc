package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.ValueType;

/**
 * A constant that is defined.  The value of the constant may be a literal, a symbol reference, or even a complex
 * expression.  A defined constant with an undefined value cannot be used directly, but can be probed for definedness.
 */
public final class DefinedConstantLiteral extends Literal {
    private final Value value;

    DefinedConstantLiteral(final Value value) {
        this.value = value;
    }

    public ValueType getType() {
        return value.getType();
    }

    public Value getValue() {
        return value;
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(final Literal other) {
        // defined constants are unique
        return this == other;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

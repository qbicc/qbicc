package org.qbicc.graph.literal;

import org.qbicc.graph.Value;
import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.ValueType;

/**
 * A constant that is defined.  The value of the constant may be a literal, a symbol reference, or even a complex
 * expression.  A defined constant with an undefined value cannot be used directly, but can be probed for definedness.
 */
public final class DefinedConstantLiteral extends Literal {
    private final String name;
    private final Value value;

    DefinedConstantLiteral(final String name, final Value value) {
        this.name = name;
        this.value = value;
    }

    public ValueType getType() {
        return value.getType();
    }

    public String getName() {
        return name;
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

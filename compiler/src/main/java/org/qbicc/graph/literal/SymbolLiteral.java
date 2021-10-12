package org.qbicc.graph.literal;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.ValueType;

/**
 * A literal representing a linkable external symbol reference.
 *
 * @deprecated Prefer {@code ProgramObject} value handles instead.
 */
@Deprecated
public final class SymbolLiteral extends Literal {
    private final String name;
    private final ValueType type;

    SymbolLiteral(final String name, final ValueType type) {
        this.name = name;
        this.type = type;
    }

    public boolean isZero() {
        return false;
    }

    public boolean equals(final Literal other) {
        return other instanceof SymbolLiteral && equals((SymbolLiteral) other);
    }

    public boolean equals(final SymbolLiteral other) {
        return this == other || other != null && name.equals(other.name) && type.equals(other.type);
    }

    public int hashCode() {
        return name.hashCode() * 19 + type.hashCode();
    }

    public ValueType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append('@').append(name);
    }
}

package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.type.ValueType;

public final class BooleanLiteral extends Literal {
    private final BooleanType type;
    private final boolean value;

    BooleanLiteral(final BooleanType type, final boolean value) {
        this.type = type;
        this.value = value;
    }

    public ValueType getType() {
        return type;
    }

    public boolean booleanValue() {
        return value;
    }

    public boolean equals(final Literal other) {
        return other instanceof BooleanLiteral && equals((BooleanLiteral) other);
    }

    public boolean equals(final BooleanLiteral other) {
        return this == other || other != null && value == other.value && type.equals(other.type);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public int hashCode() {
        return Boolean.hashCode(value);
    }

    public String toString() {
        return Boolean.toString(value);
    }
}

package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.constraint.Constraint;
import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.ValueType;

public final class IntegerLiteral extends Literal {
    private final long value;
    private final IntegerType type;

    IntegerLiteral(final IntegerType type, final long value) {
        this.value = value;
        this.type = type;
    }

    public ValueType getType() {
        return type;
    }

    public Constraint getConstraint() {
        return Constraint.equalTo(this);
    }

    public long longValue() {
        return value;
    }

    public int intValue() {
        return (int) value;
    }

    public short shortValue() {
        return (short) value;
    }

    public byte byteValue() {
        return (byte) value;
    }

    public char charValue() {
        return (char) value;
    }

    public boolean isZero() {
        return value == 0;
    }

    public boolean isOne() {
        return value == 1;
    }

    public boolean isNegative() {
        return type instanceof SignedIntegerType && value < 0;
    }

    public boolean isNotNegative() {
        return ! isNegative();
    }

    public boolean isPositive() {
        return ! isNotPositive();
    }

    public boolean isNotPositive() {
        return isNegative() || value == 0;
    }

    public boolean equals(final Literal other) {
        return other instanceof IntegerLiteral && equals((IntegerLiteral) other);
    }

    public boolean equals(final IntegerLiteral other) {
        return this == other || other != null && value == other.value && type.equals(other.type);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public String toString() {
        return type.toString(this);
    }
}

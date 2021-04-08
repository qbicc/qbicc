package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.ValueType;

public final class FloatLiteral extends Literal {
    private final FloatType type;
    private final double value;
    private final int hashCode;

    FloatLiteral(final FloatType type, final double value) {
        this.type = type;
        this.value = value;
        long bits = Double.doubleToRawLongBits(value);
        this.hashCode = (int)(bits ^ bits >>> 32) * 19 + type.hashCode();
    }

    public ValueType getType() {
        return type;
    }

    public float floatValue() {
        return (float) value;
    }

    public double doubleValue() {
        return value;
    }

    public boolean equals(final Literal other) {
        return other instanceof FloatLiteral && equals((FloatLiteral) other);
    }

    public boolean equals(final FloatLiteral other) {
        return this == other || other != null && Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(other.value) && type.equals(other.type);
    }

    public int hashCode() {
        return hashCode;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public String toString() {
        return type.toString(new StringBuilder()).append(' ').append(value).append(' ')
            .append('(').append(Double.toHexString(value)).append(')').toString();
    }

    @Override
    public boolean isDefEq(Value other) {
        return other instanceof FloatLiteral && value == ((FloatLiteral) other).value && type.equals(((FloatLiteral) other).type);
    }

    @Override
    public boolean isDefNe(Value other) {
        return other instanceof FloatLiteral && value != ((FloatLiteral) other).value && type.equals(((FloatLiteral) other).type);
    }

    @Override
    public boolean isDefLt(Value other) {
        return other instanceof FloatLiteral && value < ((FloatLiteral) other).value && type.equals(((FloatLiteral) other).type);
    }

    @Override
    public boolean isDefGt(Value other) {
        return other instanceof FloatLiteral && value > ((FloatLiteral) other).value && type.equals(((FloatLiteral) other).type);
    }

    @Override
    public boolean isDefLe(Value other) {
        return other instanceof FloatLiteral && value <= ((FloatLiteral) other).value && type.equals(((FloatLiteral) other).type);
    }

    @Override
    public boolean isDefGe(Value other) {
        return other instanceof FloatLiteral && value >= ((FloatLiteral) other).value && type.equals(((FloatLiteral) other).type);
    }

    @Override
    public boolean isDefNaN() {
        return Double.isNaN(value);
    }

    @Override
    public boolean isDefNotNaN() {
        return ! Double.isNaN(value);
    }
}

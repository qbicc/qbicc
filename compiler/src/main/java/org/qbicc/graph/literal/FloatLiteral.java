package org.qbicc.graph.literal;

import org.qbicc.graph.Value;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.WordType;

public final class FloatLiteral extends WordLiteral {
    private final FloatType type;
    private final double value;
    private final int hashCode;

    FloatLiteral(final FloatType type, final double value) {
        this.type = type;
        this.value = value;
        long bits = Double.doubleToRawLongBits(value);
        this.hashCode = (int)(bits ^ bits >>> 32) * 19 + type.hashCode();
    }

    public FloatType getType() {
        return type;
    }

    public float floatValue() {
        return (float) value;
    }

    public double doubleValue() {
        return value;
    }

    @Override
    public boolean isZero() {
        return Double.doubleToRawLongBits(value) == 0L;
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

    @Override
    Literal bitCast(LiteralFactory lf, WordType toType) {
        int minBits = type.getMinBits();
        if (toType.getMinBits() != minBits) {
            throw new IllegalArgumentException("Invalid literal bitcast between differently-sized types");
        }
        if (toType instanceof IntegerType) {
            return lf.literalOf((IntegerType) toType, minBits == 32 ? Float.floatToRawIntBits(floatValue()) : Double.doubleToRawLongBits(value));
        }
        return super.bitCast(lf, toType);
    }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return type.toString(b).append(' ').append(value).append(' ')
            .append('(').append(Double.toHexString(value)).append(')');
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

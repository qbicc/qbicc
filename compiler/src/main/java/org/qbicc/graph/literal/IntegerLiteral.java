package org.qbicc.graph.literal;

import org.qbicc.graph.Value;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.NullableType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.WordType;

public final class IntegerLiteral extends WordLiteral {
    private final long value;
    private final IntegerType type;
    private final int hashCode;

    IntegerLiteral(final IntegerType type, final long value) {
        this.value = type.truncateValue(value);
        this.type = type;
        hashCode = Long.hashCode(value) * 19 + type.hashCode();
    }

    public IntegerType getType() {
        return type;
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

    public boolean equals(final Literal other) {
        return other instanceof IntegerLiteral && equals((IntegerLiteral) other);
    }

    public boolean equals(final IntegerLiteral other) {
        return this == other || other != null && value == other.value && type.equals(other.type);
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
            return lf.literalOf((IntegerType) toType, value);
        } else if (toType instanceof FloatType) {
            return lf.literalOf((FloatType) toType, minBits == 32 ? Float.intBitsToFloat(intValue()) : Double.longBitsToDouble(longValue()));
        } else if (toType instanceof NullableType && value == 0) {
            return lf.nullLiteralOfType((NullableType) toType);
        }
        return super.bitCast(lf, toType);
    }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public String toString() {
        return type.toString(this);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append(type.toString(this));
        return b;
    }

    public boolean isZero() {
        return value == 0;
    }

    @Override
    public boolean isDefEq(Value other) {
        return equals(other);
    }

    @Override
    public boolean isDefNe(Value other) {
        return other instanceof IntegerLiteral && ! equals((IntegerLiteral) other);
    }

    @Override
    public boolean isDefLt(Value other) {
        return other instanceof IntegerLiteral && isDefLt((IntegerLiteral) other);
    }

    public boolean isDefLt(IntegerLiteral other) {
        IntegerType type = this.type;
        return type.equals(other.type) && (type instanceof SignedIntegerType ? value < other.value : Long.compareUnsigned(value, other.value) < 0);
    }

    @Override
    public boolean isDefGt(Value other) {
        return other instanceof IntegerLiteral && isDefGt((IntegerLiteral) other);
    }

    public boolean isDefGt(IntegerLiteral other) {
        IntegerType type = this.type;
        return type.equals(other.type) && (type instanceof SignedIntegerType ? value > other.value : Long.compareUnsigned(value, other.value) > 0);
    }

    @Override
    public boolean isDefLe(Value other) {
        return other instanceof IntegerLiteral && isDefLe((IntegerLiteral) other);
    }

    public boolean isDefLe(IntegerLiteral other) {
        IntegerType type = this.type;
        return type.equals(other.type) && (type instanceof SignedIntegerType ? value <= other.value : Long.compareUnsigned(value, other.value) <= 0);
    }

    @Override
    public boolean isDefGe(Value other) {
        return other instanceof IntegerLiteral && isDefGe((IntegerLiteral) other);
    }

    public boolean isDefGe(IntegerLiteral other) {
        IntegerType type = this.type;
        return type.equals(other.type) && (type instanceof SignedIntegerType ? value >= other.value : Long.compareUnsigned(value, other.value) >= 0);
    }
}

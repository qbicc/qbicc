package org.qbicc.graph.literal;

import org.qbicc.graph.Value;
import org.qbicc.graph.ValueVisitor;
import org.qbicc.graph.ValueVisitorLong;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.UnsignedIntegerType;
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

    @Override
    Literal convert(LiteralFactory lf, WordType toType) {
        if (toType instanceof IntegerType) {
            int minBits = toType.getMinBits();
            // todo: many conversions are not actually supported by the JVMS
            if (toType instanceof SignedIntegerType) {
                if (minBits == 8) {
                    return lf.literalOf((SignedIntegerType) toType, Math.min(Byte.MAX_VALUE, Math.max(Byte.MIN_VALUE, (int) value)));
                } else if (minBits == 16) {
                    return lf.literalOf((SignedIntegerType) toType, Math.min(Short.MAX_VALUE, Math.max(Short.MIN_VALUE, (int) value)));
                } else if (minBits == 32) {
                    return lf.literalOf((SignedIntegerType) toType, (int) value);
                } else if (minBits == 64) {
                    return lf.literalOf((SignedIntegerType) toType, (long) value);
                }
            } else if (toType instanceof UnsignedIntegerType) {
                if (minBits == 8) {
                    return lf.literalOf((UnsignedIntegerType) toType, Math.min(0xFF, Math.max(0, (int) value)));
                } else if (minBits == 16) {
                    return lf.literalOf((UnsignedIntegerType) toType, Math.min(0xFFFF, Math.max(0, (int) value)));
                } else if (minBits == 32) {
                    return lf.literalOf((UnsignedIntegerType) toType, Math.min(0xFFFF_FFFFL, Math.max(0, (long) value)));
                } else if (minBits == 64) {
                    // todo: no simple conversion here; just use the default for now
                }
            }
        }
        return super.convert(lf, toType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
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

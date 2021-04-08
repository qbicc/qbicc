package cc.quarkus.qcc.type;

import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class SignedIntegerType extends IntegerType {
    SignedIntegerType(final TypeSystem typeSystem, final int size, final int align, final int minBits) {
        super(typeSystem, SignedIntegerType.class.hashCode(), size, align, minBits);
    }

    public boolean equals(final IntegerType other) {
        return other instanceof SignedIntegerType && getMinBits() == other.getMinBits() && typeSystem == other.typeSystem;
    }

    public SignedIntegerType getConstraintType() {
        return this;
    }

    public SignedIntegerType asSigned() {
        return this;
    }

    public UnsignedIntegerType asUnsigned() {
        switch (minBits) {
            case 8: return typeSystem.getUnsignedInteger8Type();
            case 16: return typeSystem.getUnsignedInteger16Type();
            case 32: return typeSystem.getUnsignedInteger32Type();
            case 64: return typeSystem.getUnsignedInteger64Type();
            default: {
                throw Assert.impossibleSwitchCase(minBits);
            }
        }
    }

    public long truncateValue(final long value) {
        switch (minBits) {
            case 8: return (byte) value;
            case 16: return (short) value;
            case 32: return (int) value;
            case 64: return value;
            default: {
                throw Assert.impossibleSwitchCase(minBits);
            }
        }
    }

    @Override
    public long getMaxValue() {
        return (1L << (minBits - 1)) - 1;
    }

    @Override
    public long getMinValue() {
        return ~getMaxValue();
    }

    @Override
    public double getUpperInclusiveBound() {
        return Math.scalb(1.0, minBits - 1) - 1.0;
    }

    @Override
    public double getLowerInclusiveBound() {
        return -Math.scalb(1.0, minBits - 1);
    }

    public ValueType join(final ValueType other) {
        if (other instanceof SignedIntegerType) {
            return join((SignedIntegerType) other);
        } else {
            return super.join(other);
        }
    }

    public SignedIntegerType join(final SignedIntegerType other) {
        return minBits < other.minBits ? other : this;
    }

    public StringBuilder toString(final StringBuilder b) {
        return super.toString(b).append("s").append(minBits);
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append('s').append(minBits);
    }

    public String toString(final IntegerLiteral literal) {
        return toString(new StringBuilder()).append(' ').append(literal.longValue()).toString();
    }
}

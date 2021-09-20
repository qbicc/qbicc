package org.qbicc.type;

import org.qbicc.graph.literal.IntegerLiteral;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class UnsignedIntegerType extends IntegerType {
    UnsignedIntegerType(final TypeSystem typeSystem, final int size, final int align, final int minBits) {
        super(typeSystem, UnsignedIntegerType.class.hashCode(), size, align, minBits);
    }

    public boolean equals(final IntegerType other) {
        return other instanceof UnsignedIntegerType && getMinBits() == other.getMinBits() && typeSystem == other.typeSystem;
    }

    public UnsignedIntegerType getConstraintType() {
        return this;
    }

    public UnsignedIntegerType asUnsigned() {
        return this;
    }

    public long truncateValue(final long value) {
        switch (minBits) {
            case 8: return value & 0xffL;
            case 16: return value & 0xffffL;
            case 32: return value & 0xffffffffL;
            case 64: return value;
            default: {
                throw Assert.impossibleSwitchCase(minBits);
            }
        }
    }

    @Override
    public long getMaxValue() {
        final var numBits = minBits - 1;
        return 1L << numBits | (1L << numBits) - 1;
    }

    @Override
    public long getMinValue() {
        return 0;
    }

    @Override
    public double getUpperInclusiveBound() {
        return Math.scalb(1.0, minBits) - 1.0;
    }

    @Override
    public double getLowerInclusiveBound() {
        return 0;
    }

    public ValueType join(final ValueType other) {
        if (other instanceof UnsignedIntegerType) {
            return join((UnsignedIntegerType) other);
        } else {
            return super.join(other);
        }
    }

    public UnsignedIntegerType join(final UnsignedIntegerType other) {
        return minBits < other.minBits ? other : this;
    }

    public StringBuilder toString(final StringBuilder b) {
        return super.toString(b).append("u").append(minBits);
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append('u').append(minBits);
    }

    public String toString(final IntegerLiteral literal) {
        return toString(new StringBuilder()).append(' ').append(Long.toUnsignedString(literal.longValue())).toString();
    }

    public SignedIntegerType asSigned() {
        switch (minBits) {
            case 8: return typeSystem.getSignedInteger8Type();
            case 16: return typeSystem.getSignedInteger16Type();
            case 32: return typeSystem.getSignedInteger32Type();
            case 64: return typeSystem.getSignedInteger64Type();
            default: {
                throw Assert.impossibleSwitchCase(minBits);
            }
        }
    }
    public Primitive asPrimitive() {
        if (minBits == 16) {
            return Primitive.CHAR;
        } else {
            throw new UnsupportedOperationException();
        }
    }
}

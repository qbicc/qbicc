package cc.quarkus.qcc.type;

import cc.quarkus.qcc.graph.literal.IntegerLiteral;

/**
 * An integer type.
 */
public abstract class IntegerType extends NumericType {
    final int size;
    final int align;
    final int minBits;

    IntegerType(final TypeSystem typeSystem, final int hashCode, final int size, final int align, final int minBits) {
        super(typeSystem, (hashCode * 19 + size) * 19 + minBits);
        this.size = size;
        this.align = align;
        this.minBits = minBits;
    }

    public final boolean equals(final ValueType other) {
        return other instanceof IntegerType && equals((IntegerType) other);
    }

    public abstract boolean equals(final IntegerType other);

    public abstract IntegerType getConstraintType();

    public abstract SignedIntegerType asSigned();

    public abstract UnsignedIntegerType asUnsigned();

    public int getAlign() {
        return align;
    }

    public long getSize() {
        return size;
    }

    public int getMinBits() {
        return minBits;
    }

    /**
     * Truncate the value to this size and type.  Signed types will sign-extend.
     *
     * @param value the value to truncate
     * @return the truncated value
     */
    public abstract long truncateValue(long value);

    public abstract long getMaxValue();

    public abstract long getMinValue();

    public abstract double getUpperInclusiveBound();

    public abstract double getLowerInclusiveBound();

    public abstract String toString(final IntegerLiteral literal);
}

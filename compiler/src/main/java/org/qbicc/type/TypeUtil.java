package org.qbicc.type;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class TypeUtil {
    private TypeUtil() {}

    /**
     * Align the value up to the given alignment.
     *
     * @param value the value to align
     * @param alignment the alignment (must have exactly one bit set)
     * @return the aligned value
     */
    public static long alignUp(long value, int alignment) {
        checkAlignmentParameter("alignment", alignment);
        int mask = alignment - 1;
        return (value + mask) & ~mask;
    }

    /**
     * Check that an alignment parameter is valid.
     *
     * @param name the parameter name
     * @param value the parameter value
     */
    public static void checkAlignmentParameter(final String name, final int value) {
        Assert.checkMinimumParameter(name, 1, value);
        if (Integer.bitCount(value) != 1) {
            throw new IllegalArgumentException("Invalid alignment");
        }
    }

    /**
     * Return the next power-of-two which is greater than or equal to the given value.
     * The unsigned value must be greater than zero and no greater than {@code 1 << 31}.
     *
     * @param val the value
     * @return the next power of two
     */
    public static int nextPowerOfTwoUnsigned(int val) {
        if (val == 0) {
            throw new IllegalArgumentException("Value must be greater than zero");
        }
        final int hob = Integer.highestOneBit(val);
        if (val == hob) {
            return val;
        }
        if (hob == Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Next power of two is out of range");
        }
        return hob << 1;
    }

    /**
     * Return the next power-of-two which is greater than or equal to the given value.
     * The unsigned value must be greater than zero and no greater than {@code 1 << 63}.
     *
     * @param val the value
     * @return the next power of two
     */
    public static long nextPowerOfTwoUnsigned(long val) {
        if (val == 0) {
            throw new IllegalArgumentException("Value must be greater than zero");
        }
        final long hob = Long.highestOneBit(val);
        if (val == hob) {
            return val;
        }
        if (hob == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Next power of two is out of range");
        }
        return hob << 1;
    }
}

package cc.quarkus.qcc.type;

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
}

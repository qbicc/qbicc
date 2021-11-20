package org.qbicc.graph;

/**
 *
 */
public interface CommutativeBinaryValue extends BinaryValue {
    /**
     * Test whether the two inputs equal the given two values, in any order.
     *
     * @param v1 the first value (must not be {@code null})
     * @param v2 the second value (must not be {@code null})
     * @return {@code true} if the inputs are equal to the values, or {@code false} if either or both differ
     */
    default boolean inputsEqual(Value v1, Value v2) {
        return getLeftInput().equals(v1) && getRightInput().equals(v2)
            || getLeftInput().equals(v2) && getRightInput().equals(v1);
    }

    /**
     * Test whether either input equals the given value.
     *
     * @param v the value (must not be {@code null})
     * @return {@code true} if an input is equal to the given value, or {@code false} otherwise
     */
    default boolean eitherInputEquals(Value v) {
        return getLeftInput().equals(v) || getRightInput().equals(v);
    }
}

package org.qbicc.graph;

import org.qbicc.type.FloatType;
import org.qbicc.type.ValueType;

public interface Value extends Node {

    ValueType getType();

    <T, R> R accept(ValueVisitor<T, R> visitor, T param);

    // static

    Value[] NO_VALUES = new Value[0];

    default boolean isDefEq(Value other) {
        return equals(other) && isDefNotNaN() && other.isDefNotNaN();
    }

    default boolean isDefNe(Value other) {
        return false;
    }

    default boolean isDefLt(Value other) {
        return false;
    }

    default boolean isDefGt(Value other) {
        return false;
    }

    default boolean isDefLe(Value other) {
        return equals(other) && isDefNotNaN() && other.isDefNotNaN();
    }

    default boolean isDefGe(Value other) {
        return equals(other) && isDefNotNaN() && other.isDefNotNaN();
    }

    default boolean isDefNaN() {
        return false;
    }

    default boolean isDefNotNaN() {
        // only floats can be NaN
        return ! (getType() instanceof FloatType);
    }

    /**
     * Determine whether this value may be {@code null}.
     *
     * @return {@code true} if the value may be {@code null}, or {@code false} if it can never be {@code null}
     */
    default boolean isNullable() {
        return true;
    }

    /**
     * Determine whether this value is definitely constant. Constants are defined as literals or pure expressions whose
     * inputs are definitely constant.
     *
     * @return {@code true} if this value is definitely constant, or {@code false} if it is not definitely constant
     */
    default boolean isConstant() {
        return false;
    }
}

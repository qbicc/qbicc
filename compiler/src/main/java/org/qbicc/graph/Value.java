package org.qbicc.graph;

import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.ValueType;

public interface Value extends Node {

    ValueType getType();

    default <T extends ValueType> T getType(Class<T> expected) {
        return expected.cast(getType());
    }

    <T, R> R accept(ValueVisitor<T, R> visitor, T param);

    // static

    Value[] NO_VALUES = new Value[0];

    /**
     * Get the value that this value refers to, after stripping away any constraint nodes that are decorating it.
     *
     * @return the unconstrained value (not {@code null})
     */
    default Value unconstrained() {
        return this;
    }

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

    /**
     * Get the actual value of the given input if this value evaluates to {@code true}.
     * If the input is equal to this value, then the result must be the {@code true} literal.
     *
     * @param input the input value (must not be {@code null})
     * @return the value if {@code true} (not {@code null})
     */
    default Value getValueIfTrue(Value input) {
        if (equals(input)) {
            return this;
        }
        return input;
    }

    /**
     * Get the actual value of the given input if this value evaluates to {@code false}.
     * If the input is equal to this value, then the result must be the {@code false} literal.
     *
     * @param input the input value (must not be {@code null})
     * @return the value if {@code false} (not {@code null})
     */
    default Value getValueIfFalse(Value input) {
        if (equals(input)) {
            return this;
        }
        return input;
    }

    /**
     * Extract an element from this array value if it has a known value for the given index.
     *
     * @param lf the literal factory (must not be {@code null})
     * @param index the element index value (must not be {@code null})
     * @return the extracted value, or {@code null} if the value is not known
     */
    default Value extractElement(LiteralFactory lf, Value index) {
        return null;
    }

    /**
     * Extract a member from this compound value if it has a known value for the given member.
     *
     * @param lf the literal factory (must not be {@code null})
     * @param member the member (must not be {@code null})
     * @return the extracted value, or {@code null} if the value is not known
     */
    default Value extractMember(LiteralFactory lf, CompoundType.Member member) {
        return null;
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

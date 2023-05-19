package org.qbicc.graph;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.StructType;
import org.qbicc.type.FloatType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.NullableType;
import org.qbicc.type.PointerType;
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
     * @param bbb the basic block builder to use to build constraint nodes (must not be {@code null})
     * @param input the input value (must not be {@code null})
     * @return the value if {@code true} (not {@code null})
     */
    default Value getValueIfTrue(BasicBlockBuilder bbb, Value input) {
        if (equals(input)) {
            return this;
        }
        return input;
    }

    /**
     * Get the actual value of the given input if this value evaluates to {@code false}.
     * If the input is equal to this value, then the result must be the {@code false} literal.
     *
     * @param bbb the basic block builder to use to build constraint nodes (must not be {@code null})
     * @param input the input value (must not be {@code null})
     * @return the value if {@code false} (not {@code null})
     */
    default Value getValueIfFalse(BasicBlockBuilder bbb, Value input) {
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
    default Value extractMember(LiteralFactory lf, StructType.Member member) {
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
        return getType() instanceof NullableType;
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

    /**
     * Get the type that the referred value would have. Equivalent to {@code getPointerType(PointerType.class).getPointeeType()}.
     *
     * @return the referred value type
     */
    default ValueType getPointeeType() {
        return getType(PointerType.class).getPointeeType();
    }

    /**
     * Get the type that the referred value would have. Equivalent to {@code getPointerType(PointerType.class).getPointeeType(expected)}.
     *
     * @return the referred value type
     */
    default <T extends ValueType> T getPointeeType(Class<T> expected) {
        return getType(PointerType.class).getPointeeType(expected);
    }

    /**
     * Get the return type of this value handle. If it is not a pointer to something invokable, an exception is thrown.
     *
     * @return the return type
     */
    default ValueType getReturnType() {
        return getPointeeType(InvokableType.class).getReturnType();
    }

    /**
     * Determine whether this value points to an object which is writable.
     *
     * @return {@code true} if the target is writable, {@code false} otherwise
     */
    default boolean isWritable() {
        return getType() instanceof PointerType;
    }

    /**
     * Determine whether this value points to an object which is readable.
     *
     * @return {@code true} if the target is readable, {@code false} otherwise
     */
    default boolean isReadable() {
        return getType() instanceof PointerType;
    }

    /**
     * Determine whether this value points to a function or method that definitely does not throw any exception,
     * not even {@link StackOverflowError} or {@link OutOfMemoryError}.
     *
     * @return {@code true} if the pointer referee can definitely never throw, {@code false} otherwise
     */
    default boolean isNoThrow() {
        return false;
    }

    /**
     * Determine whether this value points to a function or method that cannot safepoint.
     *
     * @return {@code true} if the pointer referee can never safepoint, or {@code false} otherwise
     */
    default boolean isNoSafePoints() {
        return false;
    }

    /**
     * Determine whether this value points to a function or method that definitely never returns.
     *
     * @return {@code true} if the pointer referee can definitely never return, {@code false} otherwise
     */
    default boolean isNoReturn() {
        return false;
    }

    /**
     * Determine whether this value points to a function or method that definitely has no side-effects.
     *
     * @return {@code true} if the pointer referee definitely has no side-effects, {@code false} otherwise
     */
    default boolean isNoSideEffect() {
        return false;
    }

    /**
     * Determine whether this value points to a value which is constant.
     *
     * @return {@code true} if the pointer is always in the same location, or {@code false} otherwise
     */
    default boolean isPointeeConstant() {
        return false;
    }

    /**
     * Determine whether this value points to a value which is nullable.
     *
     * @return {@code true} if the pointee may be nullable, or {@code false} if the pointee is definitely not {@code null}
     */
    default boolean isPointeeNullable() {
        return true;
    }

    /**
     * Determine whether this value points to a function or method that should be constant-folded.
     *
     * @return {@code true} to constant-fold the call result, or {@code false} otherwise
     */
    default boolean isFold() {
        return false;
    }

    /**
     * Get the detected access mode for a pointer value.
     *
     * @return the detected access mode for this handle (must not be {@code null})
     */
    default AccessMode getDetectedMode() {
        return SingleUnshared;
    }

    StringBuilder toReferenceString(StringBuilder b);
}

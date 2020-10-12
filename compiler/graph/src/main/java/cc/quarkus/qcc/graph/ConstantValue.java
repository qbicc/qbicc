package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public interface ConstantValue extends Value {

    default Constraint getConstraint() {
        // todo: move to impl; make constant
        return Constraint.equalTo(this);
    }

    default void setConstraint(Constraint ignored) {
        throw Assert.unsupported();
    }

    long longValue();

    default double doubleValue() {
        return Double.longBitsToDouble(longValue());
    }

    int intValue();

    default float floatValue() {
        return Float.intBitsToFloat(intValue());
    }

    short shortValue();

    byte byteValue();

    char charValue();

    boolean isZero();

    /**
     * Determine if the value is exactly equal to one, used for some optimizations.
     *
     * @return {@code true} if the value is one
     */
    boolean isOne();

    // these are all helpful aliases for readability

    default boolean booleanValue() {
        return !isZero();
    }

    default boolean isNonZero() {
        return !isZero();
    }

    default boolean isNonNull() {
        return !isZero();
    }

    default boolean isNull() {
        return isZero();
    }

    default boolean isTrue() {
        return !isZero();
    }

    default boolean isFalse() {
        return isZero();
    }

    boolean isNegative();

    boolean isNotNegative();

    boolean isPositive();

    boolean isNotPositive();

    /**
     * Switch the type of this constant.  Note that this is an <em>unsafe</em> operation and should normally be
     * done via {@link WordType#bitCast(ConstantValue)} or similar methods when possible.
     *
     * @param type the target type (must not be {@code null})
     * @return the constant value
     */
    ConstantValue withTypeRaw(Type type);

    /**
     * Compare this constant value with another constant value with the same type.
     *
     * @param other the other constant value
     * @return the comparison result
     * @throws IllegalArgumentException if the types are not exactly the same
     */
    int compareTo(ConstantValue other) throws IllegalArgumentException;

    default <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

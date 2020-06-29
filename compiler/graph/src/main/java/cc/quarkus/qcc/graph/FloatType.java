package cc.quarkus.qcc.graph;

/**
 *
 */
public interface FloatType extends NumericType {
    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof FloatType && ((FloatType) otherType).getSize() == getSize();
    }

    // todo: are these tests meaningful and/or correct for FP types?

    default boolean isZero(long value) {
        return isNotNegative(value) && isNotPositive(value);
    }

    default boolean isZero(int value) {
        return isNotNegative(value) && isNotPositive(value);
    }

    default boolean isOne(int value) {
        return Float.intBitsToFloat(value) == 1.0f;
    }

    default boolean isOne(long value) {
        return Double.longBitsToDouble(value) == 1.0;
    }

    default boolean isOne(byte[] value) {
        throw new UnsupportedOperationException();
    }

    default Value zero() {
        return Value.const_(0.0f).withTypeRaw(this);
    }

    default boolean isNegative(final long value) {
        return Double.longBitsToDouble(value) < -0.0;
    }

    default boolean isNegative(final int value) {
        return Float.intBitsToFloat(value) < -0.0f;
    }

    default boolean isNotNegative(final long value) {
        return Double.longBitsToDouble(value) >= -0.0;
    }

    default boolean isNotNegative(final int value) {
        return Float.intBitsToFloat(value) >= -0.0f;
    }

    default boolean isPositive(final long value) {
        return Double.longBitsToDouble(value) > 0.0;
    }

    default boolean isPositive(final int value) {
        return Float.intBitsToFloat(value) > 0.0f;
    }

    default boolean isNotPositive(final long value) {
        return Double.longBitsToDouble(value) <= 0.0;
    }

    default boolean isNotPositive(final int value) {
        return Float.intBitsToFloat(value) <= 0.0f;
    }

    default int compare(final long val1, final long val2) {
        return Double.compare(Double.longBitsToDouble(val1), Double.longBitsToDouble(val2));
    }

    default int compare(final int val1, final int val2) {
        return Float.compare(Float.intBitsToFloat(val1), Float.intBitsToFloat(val2));
    }

    default int add(int v1, int v2) {
        return Float.floatToIntBits(Float.intBitsToFloat(v1) + Float.intBitsToFloat(v2));
    }

    default long add(long v1, long v2) {
        return Double.doubleToLongBits(Double.longBitsToDouble(v1) + Double.longBitsToDouble(v2));
    }

    default int subtract(int v1, int v2) {
        return Float.floatToIntBits(Float.intBitsToFloat(v1) - Float.intBitsToFloat(v2));
    }

    default long subtract(long v1, long v2) {
        return Double.doubleToLongBits(Double.longBitsToDouble(v1) - Double.longBitsToDouble(v2));
    }

    default int multiply(int v1, int v2) {
        return Float.floatToIntBits(Float.intBitsToFloat(v1) * Float.intBitsToFloat(v2));
    }

    default long multiply(long v1, long v2) {
        return Double.doubleToLongBits(Double.longBitsToDouble(v1) * Double.longBitsToDouble(v2));
    }

    default int divide(int v1, int v2) throws ArithmeticException {
        return Float.floatToIntBits(Float.intBitsToFloat(v1) / Float.intBitsToFloat(v2));
    }

    default long divide(long v1, long v2) throws ArithmeticException {
        return Double.doubleToLongBits(Double.longBitsToDouble(v1) / Double.longBitsToDouble(v2));
    }

    default int modulus(int v1, int v2) throws ArithmeticException {
        return Float.floatToIntBits(Float.intBitsToFloat(v1) % Float.intBitsToFloat(v2));
    }

    default long modulus(long v1, long v2) throws ArithmeticException {
        return Double.doubleToLongBits(Double.longBitsToDouble(v1) % Double.longBitsToDouble(v2));
    }
}

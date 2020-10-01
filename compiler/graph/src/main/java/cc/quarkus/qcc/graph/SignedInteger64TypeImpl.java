package cc.quarkus.qcc.graph;

/**
 *
 */
final class SignedInteger64TypeImpl extends SignedIntegerTypeImpl {
    SignedInteger64TypeImpl() {
        super(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public boolean isClass2Type() {
        return true;
    }

    public int getSize() {
        return 8;
    }

    public int compare(final long val1, final long val2) {
        return Long.compare(val1, val2);
    }

    public int compare(final int val1, final int val2) {
        return Long.compare(val1, val2);
    }

    public boolean isZero(final long value) {
        return value == 0;
    }

    public boolean isZero(final int value) {
        return ((long) value) == 0;
    }

    public Object boxValue(final ConstantValue value) {
        return Long.valueOf(value.longValue());
    }

    public boolean isNegative(final long value) {
        return value < 0;
    }

    public boolean isNegative(final int value) {
        return ((long) value) < 0;
    }

    public boolean isNotNegative(final long value) {
        return value >= 0;
    }

    public boolean isNotNegative(final int value) {
        return value >= 0;
    }

    public boolean isPositive(final long value) {
        return value > 0;
    }

    public boolean isPositive(final int value) {
        return value > 0;
    }

    public boolean isNotPositive(final long value) {
        return value <= 0;
    }

    public boolean isNotPositive(final int value) {
        return value <= 0;
    }

    public long and(final long v1, final long v2) {
        return v1 & v2;
    }

    public long or(final long v1, final long v2) {
        return v1 | v2;
    }

    public long xor(final long v1, final long v2) {
        return v1 ^ v2;
    }

    public long signExtend(final long value) {
        return value;
    }

    public long zeroExtend(final long value) {
        return value;
    }

    public int add(final int v1, final int v2) {
        return v1 + v2;
    }

    public long add(final long v1, final long v2) {
        return v1 + v2;
    }

    public int subtract(final int v1, final int v2) {
        return v1 - v2;
    }

    public long subtract(final long v1, final long v2) {
        return v1 - v2;
    }

    public int multiply(final int v1, final int v2) {
        return v1 * v2;
    }

    public long multiply(final long v1, final long v2) {
        return v1 * v2;
    }

    public int divide(final int v1, final int v2) throws ArithmeticException {
        return v1 / v2;
    }

    public long divide(final long v1, final long v2) throws ArithmeticException {
        return v1 / v2;
    }

    public int modulus(final int v1, final int v2) throws ArithmeticException {
        return v1 % v2;
    }

    public long modulus(final long v1, final long v2) throws ArithmeticException {
        return v1 % v2;
    }

    public String getLabelForGraph() {
        return "signed64";
    }
}

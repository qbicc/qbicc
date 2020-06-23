package cc.quarkus.qcc.graph;

/**
 *
 */
final class SignedInteger16TypeImpl extends SignedIntegerTypeImpl {
    SignedInteger16TypeImpl() {
        super(Short.MIN_VALUE, Short.MAX_VALUE);
    }

    public int getSize() {
        return 2;
    }

    public int compare(final long val1, final long val2) {
        return Short.compare((short) val1, (short) val2);
    }

    public int compare(final int val1, final int val2) {
        return Short.compare((short) val1, (short) val2);
    }

    public boolean isZero(final long value) {
        return ((short) value) == 0;
    }

    public boolean isZero(final int value) {
        return ((short) value) == 0;
    }

    public boolean isNegative(final long value) {
        return ((short) value) < 0;
    }

    public boolean isNegative(final int value) {
        return ((short) value) < 0;
    }

    public boolean isNotNegative(final long value) {
        return ((short) value) >= 0;
    }

    public boolean isNotNegative(final int value) {
        return ((short) value) >= 0;
    }

    public boolean isPositive(final long value) {
        return ((short) value) > 0;
    }

    public boolean isPositive(final int value) {
        return ((short) value) > 0;
    }

    public boolean isNotPositive(final long value) {
        return ((short) value) <= 0;
    }

    public boolean isNotPositive(final int value) {
        return ((short) value) <= 0;
    }

    public long and(final long v1, final long v2) {
        return ((short) v1) & ((short) v2);
    }

    public long or(final long v1, final long v2) {
        return ((short) v1) | ((short) v2);
    }

    public long xor(final long v1, final long v2) {
        return ((short) v1) ^ ((short) v2);
    }

    public long signExtend(final long value) {
        return (short) value;
    }

    public long zeroExtend(final long value) {
        return value & 0xffff;
    }

    public int add(final int v1, final int v2) {
        return (short) v1 + v2;
    }

    public long add(final long v1, final long v2) {
        return (short) v1 + v2;
    }

    public int subtract(final int v1, final int v2) {
        return (short) v1 - v2;
    }

    public long subtract(final long v1, final long v2) {
        return (short) v1 - v2;
    }

    public int multiply(final int v1, final int v2) {
        return (short) v1 * v2;
    }

    public long multiply(final long v1, final long v2) {
        return (short) v1 * v2;
    }

    public int divide(final int v1, final int v2) throws ArithmeticException {
        return (short) v1 / (short) v2;
    }

    public long divide(final long v1, final long v2) throws ArithmeticException {
        return (short) v1 / (short) v2;
    }

    public int modulus(final int v1, final int v2) throws ArithmeticException {
        return (short) v1 % (short) v2;
    }

    public long modulus(final long v1, final long v2) throws ArithmeticException {
        return (short) v1 % (short) v2;
    }

    public String getLabelForGraph() {
        return "signed16";
    }
}

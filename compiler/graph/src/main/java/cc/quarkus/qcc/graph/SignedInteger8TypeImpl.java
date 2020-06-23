package cc.quarkus.qcc.graph;

/**
 *
 */
final class SignedInteger8TypeImpl extends SignedIntegerTypeImpl {
    SignedInteger8TypeImpl() {
        super(Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    public int getSize() {
        return 1;
    }

    public boolean isZero(final long value) {
        return ((byte) value) == 0;
    }

    public boolean isZero(final int value) {
        return ((byte) value) == 0;
    }

    public boolean isNegative(final long value) {
        return ((byte) value) < 0;
    }

    public boolean isNegative(final int value) {
        return ((byte) value) < 0;
    }

    public boolean isNotNegative(final long value) {
        return ((byte) value) >= 0;
    }

    public boolean isNotNegative(final int value) {
        return ((byte) value) >= 0;
    }

    public boolean isPositive(final long value) {
        return ((byte) value) > 0;
    }

    public boolean isPositive(final int value) {
        return ((byte) value) > 0;
    }

    public boolean isNotPositive(final long value) {
        return ((byte) value) <= 0;
    }

    public boolean isNotPositive(final int value) {
        return ((byte) value) <= 0;
    }

    public int compare(final long val1, final long val2) {
        return Byte.compare((byte) val1, (byte) val2);
    }

    public int compare(final int val1, final int val2) {
        return Byte.compare((byte) val1, (byte) val2);
    }

    public long and(final long v1, final long v2) {
        return ((byte) v1) & ((byte) v2);
    }

    public long or(final long v1, final long v2) {
        return ((byte) v1) | ((byte) v2);
    }

    public long xor(final long v1, final long v2) {
        return ((byte) v1) ^ ((byte) v2);
    }

    public long signExtend(final long value) {
        return (byte) value;
    }

    public long zeroExtend(final long value) {
        return value & 0xff;
    }

    public int add(final int v1, final int v2) {
        return (byte) v1 + v2;
    }

    public long add(final long v1, final long v2) {
        return (byte) v1 + v2;
    }

    public int subtract(final int v1, final int v2) {
        return (byte) v1 - v2;
    }

    public long subtract(final long v1, final long v2) {
        return (byte) v1 - v2;
    }

    public int multiply(final int v1, final int v2) {
        return (byte) v1 * v2;
    }

    public long multiply(final long v1, final long v2) {
        return (byte) v1 * v2;
    }

    public int divide(final int v1, final int v2) throws ArithmeticException {
        return (byte) v1 / (byte) v2;
    }

    public long divide(final long v1, final long v2) throws ArithmeticException {
        return (byte) v1 / (byte) v2;
    }

    public int modulus(final int v1, final int v2) throws ArithmeticException {
        return (byte) v1 % (byte) v2;
    }

    public long modulus(final long v1, final long v2) throws ArithmeticException {
        return (byte) v1 % (byte) v2;
    }

    public String getLabelForGraph() {
        return "signed8";
    }
}

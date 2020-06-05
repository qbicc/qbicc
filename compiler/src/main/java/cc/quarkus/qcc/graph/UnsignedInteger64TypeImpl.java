package cc.quarkus.qcc.graph;

/**
 *
 */
final class UnsignedInteger64TypeImpl extends UnsignedIntegerTypeImpl {

    UnsignedInteger64TypeImpl() {
        super(0, -1);
    }

    public int getSize() {
        return 8;
    }

    public boolean isZero(final long value) {
        return value == 0;
    }

    public boolean isZero(final int value) {
        return ((long) value) == 0;
    }

    public boolean isNegative(final long value) {
        return false;
    }

    public boolean isNegative(final int value) {
        return false;
    }

    public boolean isNotNegative(final long value) {
        return value != 0;
    }

    public boolean isNotNegative(final int value) {
        return ((long) value) != 0;
    }

    public boolean isPositive(final long value) {
        return value != 0;
    }

    public boolean isPositive(final int value) {
        return ((long) value) != 0;
    }

    public boolean isNotPositive(final long value) {
        return value == 0;
    }

    public boolean isNotPositive(final int value) {
        return ((long) value) == 0;
    }

    public int compare(final long val1, final long val2) {
        return Long.compareUnsigned(val1, val2);
    }

    public int compare(final int val1, final int val2) {
        return Long.compareUnsigned(val1, val2);
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
        return (int) Long.divideUnsigned(v1, v2);
    }

    public long divide(final long v1, final long v2) throws ArithmeticException {
        return Long.divideUnsigned(v1, v2);
    }

    public int modulus(final int v1, final int v2) throws ArithmeticException {
        return (int) Long.remainderUnsigned(v1, v2);
    }

    public long modulus(final long v1, final long v2) throws ArithmeticException {
        return Long.remainderUnsigned(v1, v2);
    }

    public String getLabelForGraph() {
        return "unsigned64";
    }
}

package cc.quarkus.qcc.graph;

/**
 *
 */
final class UnsignedInteger16TypeImpl extends UnsignedIntegerTypeImpl {

    static final int MASK = 0xffff;

    UnsignedInteger16TypeImpl() {
        super(0, MASK);
    }

    public int getSize() {
        return 2;
    }

    public boolean isZero(final long value) {
        return ((short) value) == 0;
    }

    public boolean isZero(final int value) {
        return ((short) value) == 0;
    }

    public Object boxValue(final ConstantValue value) {
        return Character.valueOf(value.charValue());
    }

    public boolean isNegative(final long value) {
        return false;
    }

    public boolean isNegative(final int value) {
        return false;
    }

    public boolean isNotNegative(final long value) {
        return ((short) value) != 0;
    }

    public boolean isNotNegative(final int value) {
        return ((short) value) != 0;
    }

    public boolean isPositive(final long value) {
        return ((short) value) != 0;
    }

    public boolean isPositive(final int value) {
        return ((short) value) != 0;
    }

    public boolean isNotPositive(final long value) {
        return ((short) value) == 0;
    }

    public boolean isNotPositive(final int value) {
        return ((short) value) == 0;
    }

    public int compare(final long val1, final long val2) {
        return Short.compareUnsigned((short) val1, (short) val2);
    }

    public int compare(final int val1, final int val2) {
        return Short.compareUnsigned((short) val1, (short) val2);
    }

    public long and(final long v1, final long v2) {
        return v1 & v2 & MASK;
    }

    public long or(final long v1, final long v2) {
        return (v1 | v2) & MASK;
    }

    public long xor(final long v1, final long v2) {
        return (v1 ^ v2) & MASK;
    }

    public long signExtend(final long value) {
        return (short) value;
    }

    public long zeroExtend(final long value) {
        return value & MASK;
    }

    public int add(final int v1, final int v2) {
        return (v1 + v2) & MASK;
    }

    public long add(final long v1, final long v2) {
        return (v1 + v2) & MASK;
    }

    public int subtract(final int v1, final int v2) {
        return (v1 - v2) & MASK;
    }

    public long subtract(final long v1, final long v2) {
        return (v1 - v2) & MASK;
    }

    public int multiply(final int v1, final int v2) {
        return (v1 * v2) & MASK;
    }

    public long multiply(final long v1, final long v2) {
        return (v1 * v2) & MASK;
    }

    public int divide(final int v1, final int v2) throws ArithmeticException {
        return ((v1 & MASK) / (v2 & MASK)) & MASK;
    }

    public long divide(final long v1, final long v2) throws ArithmeticException {
        return ((v1 & MASK) / (v2 & MASK)) & MASK;
    }

    public int modulus(final int v1, final int v2) throws ArithmeticException {
        return ((v1 & MASK) % (v2 & MASK)) & MASK;
    }

    public long modulus(final long v1, final long v2) throws ArithmeticException {
        return ((v1 & MASK) % (v2 & MASK)) & MASK;
    }
}

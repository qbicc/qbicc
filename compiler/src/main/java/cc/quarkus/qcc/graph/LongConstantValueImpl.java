package cc.quarkus.qcc.graph;

/**
 * TEMPORARY
 */
final class LongConstantValueImpl extends ValueImpl implements ConstantValue {
    private final long value;
    private final Type type;

    LongConstantValueImpl(final long value, final Type type) {
        this.value = value;
        this.type = type;
    }

    long getValue() {
        return value;
    }

    public String getLabelForGraph() {
        return "Long:" + value;
    }

    public Type getConstantType() {
        return type;
    }

    public long longValue() {
        return value;
    }

    public int intValue() {
        return (int) value;
    }

    public short shortValue() {
        return (short) value;
    }

    public byte byteValue() {
        return (byte) value;
    }

    public char charValue() {
        return (char) value;
    }

    public boolean isZero() {
        return type.isZero(value);
    }

    public boolean isNegative() {
        return type instanceof ComparableWordType && ((ComparableWordType) type).isNegative(value);
    }

    public boolean isNotNegative() {
        return type instanceof ComparableWordType && ((ComparableWordType) type).isNotNegative(value);
    }

    public boolean isPositive() {
        return type instanceof ComparableWordType && ((ComparableWordType) type).isPositive(value);
    }

    public boolean isNotPositive() {
        return type instanceof ComparableWordType && ((ComparableWordType) type).isNotPositive(value);
    }

    public ConstantValue withTypeRaw(final Type type) {
        return new LongConstantValueImpl(value, type);
    }

    public int compareTo(final ConstantValue other) throws IllegalArgumentException {
        if (other.getConstantType() != type) {
            throw new IllegalArgumentException("Type mismatch");
        }
        if (type instanceof ComparableWordType) {
            return ((ComparableWordType) type).compare(value, other.longValue());
        } else {
            throw new IllegalArgumentException("Type is not comparable");
        }
    }
}

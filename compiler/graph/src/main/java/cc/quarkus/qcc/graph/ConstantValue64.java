package cc.quarkus.qcc.graph;

final class ConstantValue64 extends ValueImpl implements ConstantValue {
    private final long value;
    private final Type type;

    ConstantValue64(final long value, final Type type) {
        this.value = value;
        this.type = type;
    }

    long getValue() {
        return value;
    }

    public String getLabelForGraph() {
        return "Long:" + value;
    }

    public Type getType() {
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

    public boolean isOne() {
        return type.isOne(value);
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
        return new ConstantValue64(value, type);
    }

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public int compareTo(final ConstantValue other) throws IllegalArgumentException {
        if (other.getType() != type) {
            throw new IllegalArgumentException("Type mismatch");
        }
        if (type instanceof ComparableWordType) {
            return ((ComparableWordType) type).compare(value, other.longValue());
        } else {
            throw new IllegalArgumentException("Type is not comparable");
        }
    }
}

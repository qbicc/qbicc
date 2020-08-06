package cc.quarkus.qcc.graph;

final class ConstantValueEmpty extends ValueImpl implements ConstantValue {
    private final Type type;

    ConstantValueEmpty(final Type type) {
        this.type = type;
    }

    public String getLabelForGraph() {
        return "Empty";
    }

    public Type getType() {
        return type;
    }

    public long longValue() {
        return 0;
    }

    public int intValue() {
        return 0;
    }

    public short shortValue() {
        return 0;
    }

    public byte byteValue() {
        return 0;
    }

    public char charValue() {
        return 0;
    }

    public boolean isZero() {
        return true;
    }

    public boolean isOne() {
        return false;
    }

    public boolean isNegative() {
        return false;
    }

    public boolean isNotNegative() {
        return false;
    }

    public boolean isPositive() {
        return false;
    }

    public boolean isNotPositive() {
        return false;
    }

    public ConstantValue withTypeRaw(final Type type) {
        return new ConstantValueEmpty(type);
    }

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public int compareTo(final ConstantValue other) throws IllegalArgumentException {
        if (other.getType() != type) {
            throw new IllegalArgumentException("Type mismatch");
        }
        return 0;
    }
}

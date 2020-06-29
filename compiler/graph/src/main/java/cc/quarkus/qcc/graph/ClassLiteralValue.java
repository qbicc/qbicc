package cc.quarkus.qcc.graph;

final class ClassLiteralValue extends ValueImpl implements ConstantValue {
    private final ClassType classType;

    ClassLiteralValue(final ClassType classType) {
        this.classType = classType;
    }

    public Type getType() {
        return Type.CLASS;
    }

    public long longValue() {
        throw new UnsupportedOperationException();
    }

    public int intValue() {
        throw new UnsupportedOperationException();
    }

    public short shortValue() {
        throw new UnsupportedOperationException();
    }

    public byte byteValue() {
        throw new UnsupportedOperationException();
    }

    public char charValue() {
        throw new UnsupportedOperationException();
    }

    public boolean isZero() {
        return false;
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
        throw new UnsupportedOperationException();
    }

    public int compareTo(final ConstantValue other) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    public String getLabelForGraph() {
        return "class literal";
    }
}

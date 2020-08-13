package cc.quarkus.qcc.type.annotation;

/**
 * A {@code double} annotation value.
 */
public final class DoubleAnnotationValue extends PrimitiveAnnotationValue {
    private final double value;

    DoubleAnnotationValue(final double value) {
        this.value = value;
    }

    public boolean booleanValue() {
        return value != 0;
    }

    public byte byteValue() {
        return (byte) value;
    }

    public short shortValue() {
        return (short) value;
    }

    public int intValue() {
        return (int) value;
    }

    public long longValue() {
        return (long) value;
    }

    public char charValue() {
        return (char) value;
    }

    public float floatValue() {
        return (float) value;
    }

    public double doubleValue() {
        return value;
    }

    public Kind getKind() {
        return Kind.DOUBLE;
    }
}

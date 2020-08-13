package cc.quarkus.qcc.type.annotation;

/**
 * An {@code int} annotation value.
 */
public final class IntAnnotationValue extends PrimitiveAnnotationValue {
    private final int value;

    IntAnnotationValue(final int value) {
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
        return value;
    }

    public long longValue() {
        return value;
    }

    public char charValue() {
        return (char) value;
    }

    public float floatValue() {
        return value;
    }

    public double doubleValue() {
        return value;
    }

    public Kind getKind() {
        return Kind.INT;
    }
}

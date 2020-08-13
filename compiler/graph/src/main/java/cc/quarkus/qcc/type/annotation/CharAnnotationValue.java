package cc.quarkus.qcc.type.annotation;

/**
 * A {@code char} annotation value.
 */
public final class CharAnnotationValue extends PrimitiveAnnotationValue {
    private final char value;

    CharAnnotationValue(final char value) {
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
        return value;
    }

    public float floatValue() {
        return value;
    }

    public double doubleValue() {
        return value;
    }

    public Kind getKind() {
        return Kind.CHAR;
    }
}

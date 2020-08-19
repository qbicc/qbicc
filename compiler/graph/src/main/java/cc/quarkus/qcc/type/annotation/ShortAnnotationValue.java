package cc.quarkus.qcc.type.annotation;

/**
 * A {@code short} annotation value.
 */
public final class ShortAnnotationValue extends PrimitiveAnnotationValue {
    private final short value;

    ShortAnnotationValue(final short value) {
        this.value = value;
    }

    public static ShortAnnotationValue of(final int value) {
        return new ShortAnnotationValue((short) value);
    }

    public boolean booleanValue() {
        return value != 0;
    }

    public byte byteValue() {
        return (byte) value;
    }

    public short shortValue() {
        return value;
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
        return Kind.SHORT;
    }
}

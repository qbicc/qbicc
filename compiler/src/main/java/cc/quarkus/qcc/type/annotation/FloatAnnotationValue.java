package cc.quarkus.qcc.type.annotation;

/**
 * A {@code float} annotation value.
 */
public final class FloatAnnotationValue extends PrimitiveAnnotationValue {
    private final float value;

    FloatAnnotationValue(final float value) {
        this.value = value;
    }

    public static FloatAnnotationValue of(final float value) {
        return new FloatAnnotationValue(value);
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
        return value;
    }

    public double doubleValue() {
        return value;
    }

    public Kind getKind() {
        return Kind.FLOAT;
    }
}

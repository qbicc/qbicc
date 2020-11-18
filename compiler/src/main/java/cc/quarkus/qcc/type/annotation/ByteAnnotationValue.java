package cc.quarkus.qcc.type.annotation;

/**
 * A {@code byte} annotation value.
 */
public final class ByteAnnotationValue extends PrimitiveAnnotationValue {
    private final byte value;

    ByteAnnotationValue(final byte value) {
        this.value = value;
    }

    public static ByteAnnotationValue of(final int value) {
        // todo: cache
        return new ByteAnnotationValue((byte) value);
    }

    public boolean booleanValue() {
        return value != 0;
    }

    public byte byteValue() {
        return value;
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
        return Kind.BYTE;
    }
}

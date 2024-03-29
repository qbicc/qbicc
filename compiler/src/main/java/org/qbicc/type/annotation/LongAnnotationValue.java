package org.qbicc.type.annotation;

/**
 * A {@code long} annotation value.
 */
public final class LongAnnotationValue extends PrimitiveAnnotationValue {
    private final long value;

    LongAnnotationValue(final long value) {
        this.value = value;
    }

    public static LongAnnotationValue of(final long value) {
        return new LongAnnotationValue(value);
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
        return Kind.LONG;
    }
}

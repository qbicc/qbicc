package cc.quarkus.qcc.type.annotation;

/**
 * A {@code boolean} annotation value.
 */
public final class BooleanAnnotationValue extends PrimitiveAnnotationValue {
    private final boolean value;

    public static final BooleanAnnotationValue FALSE = new BooleanAnnotationValue(false);
    public static final BooleanAnnotationValue TRUE = new BooleanAnnotationValue(true);

    private BooleanAnnotationValue(final boolean value) {
        this.value = value;
    }

    public static BooleanAnnotationValue of(final boolean val) {
        return val ? TRUE : FALSE;
    }

    public boolean booleanValue() {
        return value;
    }

    public byte byteValue() {
        return (byte) intValue();
    }

    public short shortValue() {
        return (short) intValue();
    }

    public int intValue() {
        return value ? 1 : 0;
    }

    public long longValue() {
        return intValue();
    }

    public char charValue() {
        // unlikely to be useful, but maybe more useful than alternatives
        return value ? '1' : '0';
    }

    public float floatValue() {
        return intValue();
    }

    public double doubleValue() {
        return intValue();
    }

    public Kind getKind() {
        return Kind.BOOLEAN;
    }
}

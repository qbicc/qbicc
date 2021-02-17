package cc.quarkus.qcc.type;

/**
 * A floating-point type.
 */
public final class FloatType extends NumericType {
    private final int size;
    private final int bits;
    private final int align;

    FloatType(final TypeSystem typeSystem, final int size, final int bits, final int align) {
        super(typeSystem, (FloatType.class.hashCode() * 19 + size) * 19 + bits);
        this.size = size;
        this.bits = bits;
        this.align = align;
    }

    public long getSize() {
        return size;
    }

    public int getAlign() {
        return align;
    }

    public int getMinBits() {
        return bits;
    }

    public FloatType getConstraintType() {
        return this;
    }

    public ValueType join(ValueType other) {
        if (other instanceof FloatType) {
            return join((FloatType) other);
        } else {
            return super.join(other);
        }
    }

    private FloatType join(FloatType other) {
        return bits < other.bits ? other : this;
    }

    public StringBuilder toString(final StringBuilder b) {
        return super.toString(b).append("float").append(bits);
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append('f').append(bits);
    }
}

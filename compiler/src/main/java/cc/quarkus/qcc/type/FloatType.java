package cc.quarkus.qcc.type;

/**
 * A floating-point type.
 */
public final class FloatType extends NumericType {
    private final int size;
    private final int bits;
    private final int align;

    FloatType(final TypeSystem typeSystem, final int size, final int bits, final int align, final boolean const_) {
        super(typeSystem, (FloatType.class.hashCode() * 19 + size) * 19 + bits, const_);
        this.size = size;
        this.bits = bits;
        this.align = align;
    }

    ValueType constructConst() {
        return new FloatType(typeSystem, size, bits, align, true);
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

    public FloatType asConst() {
        return (FloatType) super.asConst();
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
        boolean const_ = isConst() || other.isConst();
        if (bits < other.bits) {
            return const_ ? other.asConst() : other;
        } else {
            return const_ ? asConst() : this;
        }
    }

    public StringBuilder toString(final StringBuilder b) {
        return super.toString(b).append("float").append(bits);
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append('f').append(bits);
    }
}

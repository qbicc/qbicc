package cc.quarkus.qcc.type;

/**
 *
 */
public final class BooleanType extends WordType {
    private final int size;
    private final int align;

    BooleanType(final TypeSystem typeSystem, final int size, final int align, final boolean const_) {
        super(typeSystem, BooleanType.class.hashCode() * 19 + size, const_);
        this.size = size;
        this.align = align;
    }

    public long getSize() {
        return size;
    }

    ValueType constructConst() {
        return new BooleanType(typeSystem, size, align, true);
    }

    public int getAlign() {
        return align;
    }

    public BooleanType asConst() {
        return (BooleanType) super.asConst();
    }

    public int getMinBits() {
        return typeSystem.getByteBits();
    }

    public boolean equals(final ScalarType other) {
        return other instanceof BooleanType && equals((BooleanType) other);
    }

    public boolean equals(final BooleanType other) {
        return other == this;
    }

    public StringBuilder toString(final StringBuilder b) {
        return super.toString(b).append("bool");
    }
}

package org.qbicc.type;

/**
 *
 */
public final class BooleanType extends WordType {
    private final int size;
    private final int align;

    BooleanType(final TypeSystem typeSystem, final int size, final int align) {
        super(typeSystem, BooleanType.class.hashCode() * 19 + size);
        this.size = size;
        this.align = align;
    }

    public long getSize() {
        return size;
    }

    public int getAlign() {
        return align;
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

    public ValueType join(final ValueType other) {
        if (other instanceof BooleanType) {
            return this;
        } else if (other instanceof IntegerType) {
            return other;
        } else {
            return super.join(other);
        }
    }

    public StringBuilder toString(final StringBuilder b) {
        return super.toString(b).append("bool");
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("boolean");
    }

    public Primitive asPrimitive() {
        return Primitive.BOOLEAN;
    }
}

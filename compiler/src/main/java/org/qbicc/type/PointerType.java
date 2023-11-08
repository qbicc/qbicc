package org.qbicc.type;

/**
 * A pointer to another type.  The size and behavior of a pointer type may depend on the target platform.
 */
public final class PointerType extends NullableType {
    private final ValueType pointeeType;
    private final boolean restrict;
    private final boolean constPointee;
    private final PointerType asRestrict;
    private final PointerType withConstPointee;
    private final int size;
    private final int addressSpace;
    private final PointerType asWide;

    PointerType(final TypeSystem typeSystem, final ValueType pointeeType, final boolean restrict, final boolean constPointee, int size, int addressSpace) {
        super(typeSystem, ((pointeeType.hashCode() * 19 + Boolean.hashCode(restrict)) * 19 + size) * 19 + addressSpace);
        this.pointeeType = pointeeType;
        this.restrict = restrict;
        this.constPointee = constPointee;
        this.addressSpace = addressSpace;
        this.asRestrict = restrict ? this : new PointerType(typeSystem, pointeeType, true, constPointee, size, addressSpace);
        this.withConstPointee = constPointee ? this : new PointerType(typeSystem, pointeeType, restrict, true, size, addressSpace);
        this.asWide = size == 8 ? this : new PointerType(typeSystem, pointeeType, restrict, constPointee, 8, addressSpace);
        this.size = size;
    }

    /**
     * Get the type being pointed to.
     *
     * @return the pointee type
     */
    public ValueType getPointeeType() {
        return pointeeType;
    }

    public <T extends ValueType> T getPointeeType(Class<T> expected) {
        return expected.cast(getPointeeType());
    }

    public int getAlign() {
        return typeSystem.getPointerAlignment();
    }

    public PointerType asRestrict() {
        return asRestrict;
    }

    public PointerType withConstPointee() {
        return withConstPointee;
    }

    public PointerType withQualifiersFrom(PointerType otherPointerType) {
        PointerType pointerType = this;

        if (otherPointerType.isRestrict()) {
            pointerType = pointerType.asRestrict();
        }

        if (otherPointerType.isConstPointee()) {
            pointerType = pointerType.withConstPointee();
        }

        return pointerType;
    }

    /**
     * Get a pointer type which is equivalent to this one but which has the same number of bits
     * as a Java {@code long}.
     * On 64-bit systems this will generally return the same type.
     *
     * @return the wide pointer type (not {@code null})
     */
    public PointerType asWide() {
        return asWide;
    }

    public long getSize() {
        return size;
    }

    public int addressSpace() {
        return addressSpace;
    }

    public int getMinBits() {
        return (int) (getSize()) * typeSystem.getByteBits();
    }

    public SignedIntegerType getSameSizedSignedInteger() {
        return switch (size) {
            case 4 -> typeSystem.getSignedInteger32Type();
            case 8 -> typeSystem.getSignedInteger64Type();
            default -> throw new IllegalStateException();
        };
    }

    public UnsignedIntegerType getSameSizedUnsignedInteger() {
        return switch (size) {
            case 4 -> typeSystem.getUnsignedInteger32Type();
            case 8 -> typeSystem.getUnsignedInteger64Type();
            default -> throw new IllegalStateException();
        };
    }

    public boolean isRestrict() {
        return restrict;
    }

    public boolean isConstPointee() {
        return constPointee;
    }

    public boolean equals(final ValueType other) {
        return other instanceof PointerType && equals((PointerType) other);
    }

    public boolean equals(final PointerType other) {
        return other == this || super.equals(other) && restrict == other.restrict && pointeeType.equals(other.pointeeType) && size == other.size && addressSpace == other.addressSpace;
    }

    @Override
    public ValueType join(ValueType other) {
        return other instanceof PointerType ? join((PointerType) other) : super.join(other);
    }

    public ValueType join(PointerType other) {
        ValueType pointeeType = getPointeeType().join(other.getPointeeType());
        PointerType pointerType = pointeeType.getPointer();
        boolean restrict = this.restrict || other.restrict;
        boolean constPointee = this.constPointee || other.constPointee;

        if (restrict) {
            pointerType = pointerType.asRestrict();
        }

        if (constPointee) {
            pointerType = pointerType.withConstPointee();
        }

        return pointerType;
    }

    public StringBuilder toString(final StringBuilder b) {
        super.toString(b);
        if (restrict) {
            b.append("restrict ");
        }
        if (size == 8 && typeSystem.getPointerSize() < 8) {
            b.append("wide ");
        }
        b.append("pointer ");
        if (addressSpace != 0) {
            b.append("addrspace(").append(addressSpace).append(')');
        }
        b.append(" to ");
        if (constPointee) {
            b.append("const ");
        }
        pointeeType.toString(b);
        return b;
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        pointeeType.toFriendlyString(b);
        if (addressSpace != 0) {
            b.append(' ').append("addrspace(").append(addressSpace).append(')');
        }
        b.append('*');
        return b;
    }

    public PointerType getConstraintType() {
        return this;
    }
}

package org.qbicc.type;

/**
 * A pointer to another type.  The size and behavior of a pointer type may depend on the target platform.
 */
public final class PointerType extends NullableType {
    private final ValueType pointeeType;
    private final boolean restrict;
    private final boolean constPointee;
    private final boolean collected;
    private final PointerType asRestrict;
    private final PointerType withConstPointee;
    private final PointerType asCollected;
    private final int size;
    private final PointerType asWide;

    PointerType(final TypeSystem typeSystem, final ValueType pointeeType, final boolean restrict, final boolean constPointee, final boolean collected, int size) {
        super(typeSystem, pointeeType.hashCode() * 19 + Boolean.hashCode(restrict));
        this.pointeeType = pointeeType;
        this.restrict = restrict;
        this.constPointee = constPointee;
        this.collected = collected;
        this.asRestrict = restrict ? this : new PointerType(typeSystem, pointeeType, true, constPointee, collected, size);
        this.withConstPointee = constPointee ? this : new PointerType(typeSystem, pointeeType, restrict, true, collected, size);
        this.asCollected = collected ? this : new PointerType(typeSystem, pointeeType, restrict, constPointee, true, size);
        this.asWide = size == 8 ? this : new PointerType(typeSystem, pointeeType, restrict, constPointee, collected, 8);
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

    public int getAlign() {
        return typeSystem.getPointerAlignment();
    }

    public PointerType asRestrict() {
        return asRestrict;
    }

    public PointerType withConstPointee() {
        return withConstPointee;
    }

    public PointerType asCollected() {
        return asCollected;
    }

    public PointerType withQualifiersFrom(PointerType otherPointerType) {
        PointerType pointerType = this;

        if (otherPointerType.isRestrict()) {
            pointerType = pointerType.asRestrict();
        }

        if (otherPointerType.isConstPointee()) {
            pointerType = pointerType.withConstPointee();
        }

        if (otherPointerType.isCollected()) {
            pointerType = pointerType.asCollected();
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

    public int getMinBits() {
        return (int) (getSize()) * typeSystem.getByteBits();
    }

    public SignedIntegerType getSameSizedSignedInteger() {
        return switch (size) {
            case 32 -> typeSystem.getSignedInteger32Type();
            case 64 -> typeSystem.getSignedInteger64Type();
            default -> throw new IllegalStateException();
        };
    }

    public UnsignedIntegerType getSameSizedUnsignedInteger() {
        return switch (size) {
            case 32 -> typeSystem.getUnsignedInteger32Type();
            case 64 -> typeSystem.getUnsignedInteger64Type();
            default -> throw new IllegalStateException();
        };
    }

    public boolean isRestrict() {
        return restrict;
    }

    public boolean isConstPointee() {
        return constPointee;
    }

    public boolean isCollected() {
        return collected;
    }

    public boolean equals(final ValueType other) {
        return other instanceof PointerType && equals((PointerType) other);
    }

    public boolean equals(final PointerType other) {
        return other == this || super.equals(other) && restrict == other.restrict && collected == other.collected && pointeeType.equals(other.pointeeType);
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

        if (collected != other.collected) {
            throw new IllegalArgumentException("Cannot join non-collected and collected pointer types");
        }

        if (restrict) {
            pointerType = pointerType.asRestrict();
        }

        if (constPointee) {
            pointerType = pointerType.withConstPointee();
        }

        if (collected) {
            pointerType = pointerType.asCollected();
        }

        return pointerType;
    }

    public StringBuilder toString(final StringBuilder b) {
        super.toString(b);
        if (collected) {
            b.append("collected ");
        }
        if (restrict) {
            b.append("restrict ");
        }
        if (size == 8 && typeSystem.getPointerSize() < 8) {
            b.append("wide ");
        }
        b.append("pointer to ");
        if (constPointee) {
            b.append("const ");
        }
        pointeeType.toString(b);
        return b;
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return pointeeType.toFriendlyString(b).append('*');
    }

    public PointerType getConstraintType() {
        return this;
    }
}

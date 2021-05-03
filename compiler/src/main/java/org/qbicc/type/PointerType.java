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

    PointerType(final TypeSystem typeSystem, final ValueType pointeeType, final boolean restrict, final boolean constPointee, final boolean collected) {
        super(typeSystem, pointeeType.hashCode() * 19 + Boolean.hashCode(restrict));
        this.pointeeType = pointeeType;
        this.restrict = restrict;
        this.constPointee = constPointee;
        this.collected = collected;
        this.asRestrict = restrict ? this : new PointerType(typeSystem, pointeeType, true, constPointee, collected);
        this.withConstPointee = constPointee ? this : new PointerType(typeSystem, pointeeType, restrict, true, collected);
        this.asCollected = collected ? this : new PointerType(typeSystem, pointeeType, restrict, constPointee, true);
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

    public long getSize() {
        return typeSystem.getPointerSize();
    }

    public int getMinBits() {
        return (int) (getSize()) * typeSystem.getByteBits();
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
        b.append("pointer to ");
        if (constPointee) {
            b.append("const ");
        }
        pointeeType.toString(b);
        return b;
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return pointeeType.toFriendlyString(b.append("ptr."));
    }

    public PointerType getConstraintType() {
        return this;
    }
}

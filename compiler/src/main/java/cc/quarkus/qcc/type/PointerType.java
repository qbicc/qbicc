package cc.quarkus.qcc.type;

/**
 * A pointer to another type.  The size and behavior of a pointer type may depend on the target platform.
 */
public final class PointerType extends WordType {
    private final ValueType pointeeType;
    private final boolean restrict;
    private final PointerType asRestrict;

    PointerType(final TypeSystem typeSystem, final ValueType pointeeType, final boolean restrict) {
        super(typeSystem, pointeeType.hashCode() * 19 + Boolean.hashCode(restrict));
        this.pointeeType = pointeeType;
        this.restrict = restrict;
        this.asRestrict = restrict ? this : new PointerType(typeSystem, pointeeType, true);
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

    public boolean isImplicitlyConvertibleFrom(final Type other) {
        return super.isImplicitlyConvertibleFrom(other) || other instanceof ArrayType && ((ArrayType) other).getElementType().equals(pointeeType);
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

    public boolean equals(final ValueType other) {
        return other instanceof PointerType && equals((PointerType) other);
    }

    public boolean equals(final PointerType other) {
        return other == this || super.equals(other) && restrict == other.restrict && pointeeType.equals(other.pointeeType);
    }

    @Override
    public ValueType join(ValueType other) {
        return other instanceof PointerType ? join((PointerType) other) : super.join(other);
    }

    public ValueType join(PointerType other) {
        ValueType pointeeType = getPointeeType().join(other.getPointeeType());
        PointerType pointerType = pointeeType.getPointer();
        boolean restrict = this.restrict || other.restrict;
        // boolean constPointee = this.constPointee || other.constPointee;
        return restrict ? pointerType.asRestrict() : pointerType;
    }

    public StringBuilder toString(final StringBuilder b) {
        super.toString(b);
        if (restrict) {
            b.append("restrict ");
        }
        b.append("pointer to ");
        // todo: const?
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

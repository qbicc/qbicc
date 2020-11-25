package cc.quarkus.qcc.type;

/**
 * A pointer to another type.  The size and behavior of a pointer type may depend on the target platform.
 */
public final class PointerType extends WordType {
    private final Type pointeeType;
    private final boolean restrict;
    private final PointerType asRestrict;

    PointerType(final TypeSystem typeSystem, final Type pointeeType, final boolean restrict, final boolean const_) {
        super(typeSystem, pointeeType.hashCode() * 19 + Boolean.hashCode(restrict), const_);
        this.pointeeType = pointeeType;
        this.restrict = restrict;
        this.asRestrict = restrict ? this : new PointerType(typeSystem, pointeeType, true, const_);
    }

    /**
     * Get the type being pointed to.
     *
     * @return the pointee type
     */
    public Type getPointeeType() {
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

    ValueType constructConst() {
        return new PointerType(typeSystem, pointeeType, restrict, true);
    }

    public PointerType asConst() {
        return (PointerType) super.asConst();
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

    public StringBuilder toString(final StringBuilder b) {
        super.toString(b);
        if (restrict) {
            b.append("restrict ");
        }
        b.append("pointer to ");
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

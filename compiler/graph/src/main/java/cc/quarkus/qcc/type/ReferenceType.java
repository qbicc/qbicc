package cc.quarkus.qcc.type;

import cc.quarkus.qcc.graph.literal.TypeIdLiteral;

/**
 * A reference type.  A reference is essentially an abstract representation of an encoded pointer to an object.  The
 * pointee contains, somewhere, a value of type {@link TypeIdType} representing the object's polymorphic
 * type.  Alternatively, a reference value may be equal to {@code null}.
 */
public final class ReferenceType extends ValueType {
    private final TypeIdLiteral upperBound;
    private final int size;
    private final int align;

    ReferenceType(final TypeSystem typeSystem, final TypeIdLiteral upperBound, final int size, final int align, final boolean const_) {
        super(typeSystem, size * 19 + ReferenceType.class.hashCode(), const_);
        this.upperBound = upperBound;
        this.size = size;
        this.align = align;
    }

    public ReferenceType getConstraintType() {
        return this;
    }

    public long getSize() {
        return size;
    }

    /**
     * Get the declared upper bound of this reference type.
     *
     * @return the upper bound
     */
    public TypeIdLiteral getUpperBound() {
        return upperBound;
    }

    ValueType constructConst() {
        return new ReferenceType(typeSystem, upperBound, size, align, true);
    }

    public ReferenceType asConst() {
        return (ReferenceType) super.asConst();
    }

    public int getAlign() {
        return align;
    }

    public boolean equals(final ValueType other) {
        return other instanceof ReferenceType && equals((ReferenceType) other);
    }

    public boolean equals(final ReferenceType other) {
        return this == other || other != null && size == other.size && align == other.align && upperBound.equals(other.upperBound);
    }

    public StringBuilder toString(final StringBuilder b) {
        return super.toString(b).append("reference");
    }
}

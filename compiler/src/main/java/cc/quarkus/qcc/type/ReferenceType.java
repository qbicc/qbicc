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
    private final boolean nullable;
    private final ReferenceType asNullable;

    ReferenceType(final TypeSystem typeSystem, final TypeIdLiteral upperBound, final boolean nullable, final int size, final int align, final boolean const_) {
        super(typeSystem, size * 19 + ReferenceType.class.hashCode() * Boolean.hashCode(nullable), const_);
        this.upperBound = upperBound;
        this.size = size;
        this.align = align;
        this.nullable = nullable;
        this.asNullable = nullable ? this : new ReferenceType(typeSystem, upperBound, true, size, align, const_);
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

    public boolean isNullable() {
        return nullable;
    }

    public ReferenceType asNullable() {
        return asNullable;
    }

    ValueType constructConst() {
        return new ReferenceType(typeSystem, upperBound, nullable, size, align, true);
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

    public ValueType join(final ValueType other) {
        if (other instanceof ReferenceType) {
            return join(((ReferenceType) other));
        } else if (other instanceof NullType) {
            return asNullable();
        } else {
            return super.join(other);
        }
    }

    public ReferenceType join(final ReferenceType other) {
        boolean const_ = isConst() || other.isConst();
        boolean nullable = isNullable() || other.isNullable();
        ReferenceType result;
        if (upperBound.isSupertypeOf(other.upperBound)) {
            result = this;
        } else if (upperBound.isSubtypeOf(other.upperBound)) {
            result = other;
        } else {
            // find a common supertype of both
            if (upperBound.hasSuperClass()) {
                result = typeSystem.getReferenceType(upperBound.getSuperClass());
                if (const_) result = asConst();
                if (nullable) result = asNullable();
                return other.join(result);
            } else if (other.upperBound.hasSuperClass()) {
                result = typeSystem.getReferenceType(other.upperBound.getSuperClass());
                if (const_) result = asConst();
                if (nullable) result = asNullable();
                return result.join(other);
            } else {
                // should not be possible because one or the other is j.l.Object
                return (ReferenceType) super.join(other);
            }
        }
        if (const_) result = asConst();
        if (nullable) result = asNullable();
        return result;
    }

    public StringBuilder toString(final StringBuilder b) {
        super.toString(b);
        if (nullable) {
            b.append("nullable ");
        }
        b.append("reference");
        return b.append("(").append(upperBound).append(")");
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        // todo: use the actual numerical value of the type literal if possible at this stage, else use encoding scheme
        return b.append("ref.").append(Integer.toHexString(upperBound.hashCode()));
    }
}

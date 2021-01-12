package cc.quarkus.qcc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A reference type.  A reference is essentially an abstract representation of an encoded pointer to an object.  The
 * pointee contains, somewhere, a value of type {@link TypeType} representing the object's polymorphic
 * type.  Alternatively, a reference value may be equal to {@code null}.
 */
public final class ReferenceType extends WordType {
    private static final VarHandle refArrayTypeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "refArrayType", VarHandle.class, ReferenceType.class, ReferenceArrayObjectType.class);

    private final ObjectType upperBound;
    private final int size;
    private final int align;
    private final boolean nullable;
    private final ReferenceType asNullable;
    @SuppressWarnings("unused")
    private volatile ReferenceArrayObjectType refArrayType;

    ReferenceType(final TypeSystem typeSystem, final ObjectType upperBound, final boolean nullable, final int size, final int align, final boolean const_) {
        super(typeSystem, size * 19 + ReferenceType.class.hashCode() * Boolean.hashCode(nullable), const_);
        this.upperBound = upperBound;
        this.size = size;
        this.align = align;
        this.nullable = nullable;
        this.asNullable = nullable ? this : new ReferenceType(typeSystem, this.upperBound, true, size, align, const_);
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
    public ObjectType getUpperBound() {
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

    public int getMinBits() {
        return typeSystem.getReferenceSize() * typeSystem.getByteBits();
    }

    /**
     * Get the referenceArrayObject type to this type.
     *
     * @return the referenceArrayObject type
     */
    public final ReferenceArrayObjectType getReferenceArrayObject() {
        ReferenceArrayObjectType refArrayType = this.refArrayType;
        if (refArrayType != null) {
            return refArrayType;
        }
        ReferenceArrayObjectType newReferenceArrayObjectType = typeSystem.createReferenceArrayObject(this);
        while (! refArrayTypeHandle.compareAndSet(this, null, newReferenceArrayObjectType)) {
            refArrayType = this.refArrayType;
            if (refArrayType != null) {
                return refArrayType;
            }
        }
        return newReferenceArrayObjectType;
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
                result = upperBound.getSuperClassType().getReference();
                if (const_) result = result.asConst();
                if (nullable) result = result.asNullable;
                return other.join(result);
            } else if (other.upperBound.hasSuperClass()) {
                result = other.upperBound.getSuperClassType().getReference();
                if (const_) result = result.asConst();
                if (nullable) result = result.asNullable;
                return result.join(this);
            } else {
                // both are j.l.Object
                result = this;
            }
        }
        if (const_) result = asConst();
        if (nullable) result = asNullable();
        return result;
    }

    public StringBuilder toString(final StringBuilder b) {
        super.toString(b);
        if (nullable) {
            b.append("nullable").append(' ');
        }
        b.append("reference");
        return upperBound.toString(b.append('(')).append(')');
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return upperBound.toFriendlyString(b.append("ref").append('.'));
    }
}

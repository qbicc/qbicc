package cc.quarkus.qcc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * A reference type.  A reference is essentially an abstract representation of an encoded pointer to an object.  The
 * pointee contains, somewhere, a value of type {@link TypeType} representing the object's polymorphic
 * type.  Alternatively, a reference value may be equal to {@code null}.
 */
public final class ReferenceType extends WordType {
    private static final VarHandle refArrayTypeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "refArrayType", VarHandle.class, ReferenceType.class, ReferenceArrayObjectType.class);

    private final PhysicalObjectType upperBound;
    private final Set<InterfaceObjectType> interfaceBounds;
    private final int size;
    private final int align;
    private final boolean nullable;
    private final ReferenceType asNullable;
    @SuppressWarnings("unused")
    private volatile ReferenceArrayObjectType refArrayType;

    ReferenceType(final TypeSystem typeSystem, final PhysicalObjectType upperBound, Set<InterfaceObjectType> interfaceBounds, final boolean nullable, final int size, final int align, final boolean const_) {
        super(typeSystem, ((Objects.hash(upperBound, interfaceBounds) * 19 + size) * 19 + ReferenceType.class.hashCode()) * 19 + Boolean.hashCode(nullable), const_);
        this.upperBound = upperBound;
        this.interfaceBounds = interfaceBounds;
        this.size = size;
        this.align = align;
        this.nullable = nullable;
        this.asNullable = nullable ? this : new ReferenceType(typeSystem, this.upperBound, interfaceBounds, true, size, align, const_);
    }

    public ReferenceType getConstraintType() {
        return this;
    }

    public long getSize() {
        return size;
    }

    /**
     * Get the declared physical upper bound of this reference type.
     *
     * @return the upper bound
     */
    public PhysicalObjectType getUpperBound() {
        return upperBound;
    }

    /**
     * Get the declared upper interface bounds of this reference type.
     *
     * @return the declared upper interface bounds (must not be {@code null})
     */
    public Set<InterfaceObjectType> getInterfaceBounds() {
        return interfaceBounds;
    }

    public boolean isNullable() {
        return nullable;
    }

    public ReferenceType asNullable() {
        return asNullable;
    }

    ValueType constructConst() {
        return new ReferenceType(typeSystem, upperBound, interfaceBounds, nullable, size, align, true);
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
        return this == other || super.equals(other) && size == other.size && align == other.align && upperBound.equals(other.upperBound) && interfaceBounds.equals(other.interfaceBounds);
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
        ReferenceType result = getUpperBound().getCommonSupertype(other.getUpperBound()).getReference();
        if (const_) result = result.asConst();
        if (nullable) result = result.asNullable();
        return result;
    }

    /**
     * Get a new reference type whose upper bound has been narrowed to the given type, or {@code null} if
     * such a narrowing is impossible.
     * It is always possible to narrow to an interface type.
     *
     * @param otherType the type to narrow to (must not be {@code null})
     * @return the narrowed reference type, or {@code null} if the narrowing is impossible
     */
    public ReferenceType narrow(ObjectType otherType) {
        if (otherType instanceof PhysicalObjectType) {
            return narrow((PhysicalObjectType) otherType);
        } else {
            assert otherType instanceof InterfaceObjectType;
            return narrow((InterfaceObjectType) otherType);
        }
    }

    /**
     * Get a new reference type whose upper bound has been narrowed to the given type, or {@code null} if
     * such a narrowing is impossible.
     *
     * @param otherType the type to narrow to (must not be {@code null})
     * @return the narrowed reference type, or {@code null} if the narrowing is impossible
     */
    public ReferenceType narrow(PhysicalObjectType otherType) {
        PhysicalObjectType upperBound = getUpperBound();
        if (otherType.isSupertypeOf(upperBound)) {
            return this;
        } else if (otherType.isSubtypeOf(upperBound)) {
            return new ReferenceType(typeSystem, otherType, filtered(interfaceBounds, otherType), nullable, size, align, isConst());
        } else {
            // no valid narrowing
            return null;
        }
    }

    /**
     * Get a new reference type whose upper bound has been narrowed to the given type.
     * It is always possible to narrow to an interface type.
     *
     * @param otherType the type to narrow to (must not be {@code null})
     * @return the narrowed reference type (not {@code null})
     */
    public ReferenceType narrow(InterfaceObjectType otherType) {
        PhysicalObjectType upperBound = getUpperBound();
        if (instanceOf(otherType)) {
            // already implicitly narrowed to this type
            return this;
        } else {
            return new ReferenceType(typeSystem, upperBound, filteredWith(interfaceBounds, otherType), nullable, size, align, isConst());
        }
    }

    private static final InterfaceObjectType[] EMPTY = new InterfaceObjectType[0];

    private Set<InterfaceObjectType> filtered(Set<InterfaceObjectType> set, PhysicalObjectType other) {
        InterfaceObjectType[] array = set.toArray(EMPTY);
        int len = array.length;
        int newSize = 0;
        for (int i = 0; i < len; i ++) {
            if (other.isSubtypeOf(array[i])) {
                // no longer needed in our set
                array[i] = null;
            } else {
                newSize++;
            }
        }
        if (newSize == len) {
            // we didn't change the set
            return set;
        } else if (newSize == 0) {
            // save an allocation
            return Set.of();
        }
        InterfaceObjectType[] newArray = new InterfaceObjectType[newSize];
        for (int i = 0, j = 0; i < len; i++) {
            InterfaceObjectType item = array[i];
            if (item != null) {
                newArray[j++] = item;
            }
        }
        return Set.of(newArray);
    }

    private Set<InterfaceObjectType> filteredWith(Set<InterfaceObjectType> set, InterfaceObjectType other) {
        InterfaceObjectType[] array = set.toArray(EMPTY);
        int len = array.length;
        int newSize = 0;
        boolean add = true;
        for (int i = 0; i < len; i ++) {
            if (other.isSubtypeOf(array[i])) {
                // no longer needed in our set
                array[i] = null;
            } else if (array[i].isSubtypeOf(other)) {
                // we already have `other` in our set
                add = false;
                newSize++;
            } else {
                newSize++;
            }
        }
        if (newSize == len && ! add) {
            // we didn't change the set
            return set;
        } else if (newSize == 0) {
            // save an allocation
            return add ? Set.of(other) : Set.of();
        }
        if (add) {
            newSize ++;
        }
        InterfaceObjectType[] newArray = new InterfaceObjectType[newSize];
        int j = 0;
        for (int i = 0; i < len; i++) {
            InterfaceObjectType item = array[i];
            if (item != null) {
                newArray[j++] = item;
            }
        }
        if (add) {
            newArray[j] = other;
        }
        return Set.of(newArray);
    }

    /**
     * Determine whether the target of this reference type statically implements the given type.
     *
     * @param other the other type (must not be {@code null})
     * @return {@code true} if the reference target implements the given type, or {@code false} otherwise
     */
    public boolean instanceOf(ObjectType other) {
        if (other instanceof PhysicalObjectType) {
            return instanceOf((PhysicalObjectType) other);
        } else {
            assert other instanceof InterfaceObjectType;
            return instanceOf((InterfaceObjectType) other);
        }
    }

    /**
     * Determine whether the target of this reference type statically implements the given type.
     *
     * @param other the other type (must not be {@code null})
     * @return {@code true} if the reference target implements the given type, or {@code false} otherwise
     */
    public boolean instanceOf(PhysicalObjectType other) {
        return getUpperBound().isSubtypeOf(other);
    }

    /**
     * Determine whether the target of this reference type statically implements the given type.
     *
     * @param other the other type (must not be {@code null})
     * @return {@code true} if the reference target implements the given type, or {@code false} otherwise
     */
    public boolean instanceOf(InterfaceObjectType other) {
        if (getUpperBound().isSubtypeOf(other)) {
            return true;
        }
        for (InterfaceObjectType bound : interfaceBounds) {
            if (bound.isSubtypeOf(other)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine whether the target of this reference type statically implements all of the bounds of the given type.
     *
     * @param other the other reference type (must not be {@code null})
     * @return {@code true} if the reference target implements all of the bounds of the given type, or {@code false} otherwise
     */
    public boolean instanceOf(ReferenceType other) {
        if (! instanceOf(other.getUpperBound())) {
            return false;
        }
        for (InterfaceObjectType bound : other.getInterfaceBounds()) {
            if (! instanceOf(bound)) {
                return false;
            }
        }
        return true;
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

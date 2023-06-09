package org.qbicc.type;

import java.util.Objects;
import java.util.Set;

/**
 * A reference type.  A reference is essentially an abstract representation of an encoded pointer to an object.  The
 * pointee contains, somewhere, a value of type {@link TypeIdType} representing the object's polymorphic
 * type.  Alternatively, a reference value may be equal to {@code null}.
 */
public final class ReferenceType extends NullableType {
    private final PhysicalObjectType upperBound;
    private final Set<InterfaceObjectType> interfaceBounds;
    private final int align;

    ReferenceType(final TypeSystem typeSystem, final PhysicalObjectType upperBound, final Set<InterfaceObjectType> interfaceBounds, final int align) {
        super(typeSystem, (Objects.hash(upperBound, interfaceBounds) * 19 + typeSystem.getReferenceSize()) * 19 + ReferenceType.class.hashCode());
        this.upperBound = upperBound;
        this.interfaceBounds = interfaceBounds;
        this.align = align;
    }

    public ReferenceType getConstraintType() {
        return this;
    }

    public long getSize() {
        return typeSystem.getReferenceSize();
    }

    public SignedIntegerType getSameSizedSignedInteger() {
        return switch ((int) getSize()) {
            case 32 -> typeSystem.getSignedInteger32Type();
            case 64 -> typeSystem.getSignedInteger64Type();
            default -> throw new IllegalStateException();
        };
    }

    public UnsignedIntegerType getSameSizedUnsignedInteger() {
        return switch ((int) getSize()) {
            case 32 -> typeSystem.getUnsignedInteger32Type();
            case 64 -> typeSystem.getUnsignedInteger64Type();
            default -> throw new IllegalStateException();
        };
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

    public int getMinBits() {
        return typeSystem.getReferenceSize() * typeSystem.getByteBits();
    }

    public int getAlign() {
        return align;
    }

    public boolean equals(final ValueType other) {
        return other instanceof ReferenceType && equals((ReferenceType) other);
    }

    public boolean equals(final ReferenceType other) {
        return this == other || super.equals(other) && align == other.align && upperBound.equals(other.upperBound) && interfaceBounds.equals(other.interfaceBounds);
    }

    public boolean isImplicitlyConvertibleFrom(Type other) {
        return other instanceof ReferenceType && isImplicitlyConvertibleFrom((ReferenceType) other);
    }

    public boolean isImplicitlyConvertibleFrom(ReferenceType other) {
        return other.instanceOf(this);
    }

    public ValueType join(final ValueType other) {
        if (other instanceof ReferenceType) {
            return join(((ReferenceType) other));
        } else {
            return super.join(other);
        }
    }

    public ReferenceType join(final ReferenceType other) {
        return getUpperBound().getCommonSupertype(other.getUpperBound()).getReference();
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
            return new ReferenceType(typeSystem, otherType, filtered(interfaceBounds, otherType), align);
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
            return new ReferenceType(typeSystem, upperBound, filteredWith(interfaceBounds, otherType), align);
        }
    }

    private static final InterfaceObjectType[] EMPTY = new InterfaceObjectType[0];

    private Set<InterfaceObjectType> filtered(Set<InterfaceObjectType> set, PhysicalObjectType other) {
        if (set.isEmpty()) {
            return set;
        }
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
        if (set.isEmpty()) {
            return Set.of(other);
        }
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
        for (InterfaceObjectType item : array) {
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
     * Return a type that is a subtype of both this and the given type, or {@code null} if such a type is not possible.
     *
     * @param otherType the type to meet (must not be {@code null})
     * @return the subtype of both types, or {@code null} if no such type exists
     */
    public ReferenceType meet(ReferenceType otherType) {
        if (this == otherType) {
            return this;
        }
        PhysicalObjectType upperBound = getUpperBound();
        PhysicalObjectType otherUpperBound = otherType.getUpperBound();
        if (otherUpperBound.isSupertypeOf(upperBound)) {
            // keep upper bound as-is
        } else if (otherUpperBound.isSubtypeOf(upperBound)) {
            // use other type's upper bound
            return otherType.meet(this);
        } else {
            // no common bound at all
            return null;
        }
        // the rest is interfaces...
        Set<InterfaceObjectType> interfaceBounds = getInterfaceBounds();
        Set<InterfaceObjectType> otherInterfaceBounds = otherType.getInterfaceBounds();
        // calculate the true union, filtering out other bounds that are present on our upper bound's interface set
        Set<InterfaceObjectType> union = union(interfaceBounds, otherInterfaceBounds, upperBound);
        if (union == interfaceBounds) {
            // they were equal
            return this;
        }
        return new ReferenceType(typeSystem, upperBound, union, align);
    }

    private Set<InterfaceObjectType> union(Set<InterfaceObjectType> ours, Set<InterfaceObjectType> others, PhysicalObjectType upperBound) {
        if (others.isEmpty()) {
            // ours is already filtered by our own upper bound
            return ours;
        } else if (ours.isEmpty()) {
            // others is *not yet* filtered by our upper bound
            return filtered(others, upperBound);
        }
        // this is unfortunately an n×m operation, but we can short circuit many cases
        InterfaceObjectType[] ourArray = ours.toArray(EMPTY);
        InterfaceObjectType[] otherArray = others.toArray(EMPTY);
        int ourSize = ourArray.length;
        int otherSize = otherArray.length;
        int write = 0;
        // spin through otherArray first, punch out holes for every bound covered by ours or by upperBound
        nextOther: for (int i = 0; i < otherSize; i ++) {
            InterfaceObjectType theirItem = otherArray[i];
            if (upperBound.isSubtypeOf(theirItem)) {
                // our upper bound already includes this one, so skip it
                otherArray[i] = null;
                continue;
            }
            for (InterfaceObjectType ourItem : ourArray) {
                if (ourItem.isSubtypeOf(theirItem)) {
                    // ours is better than theirs; drop theirs
                    otherArray[i] = null;
                    continue nextOther;
                }
            }
            // keep theirs
            write ++;
        }
        // now spin through ourArray and punch out holes for bounds covered by theirs
        nextOurs: for (int i = 0; i < ourSize; i++) {
            InterfaceObjectType ourItem = ourArray[i];
            for (InterfaceObjectType theirItem : otherArray) {
                if (theirItem != null && theirItem.isSubtypeOf(ourItem)) {
                    // theirs is better than ours; drop ours
                    ourArray[i] = null;
                    continue nextOurs;
                }
            }
            // keep ours
            write ++;
        }

        if (write == 0) {
            // none retained
            return Set.of();
        }
        InterfaceObjectType[] newArray = new InterfaceObjectType[write];
        int j = 0;
        for (int i = 0; i < ourSize; i ++) {
            if (ourArray[i] != null) {
                newArray[j ++] = ourArray[i];
            }
        }
        for (int i = 0; i < otherSize; i ++) {
            if (otherArray[i] != null) {
                newArray[j ++] = otherArray[i];
            }
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
        b.append("reference");
        upperBound.toString(b.append('('));
        for (InterfaceObjectType interfaceBound : interfaceBounds) {
            b.append('&').append(interfaceBound);
        }
        return b.append(')');
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        b.append("ref<");
        Set<InterfaceObjectType> interfaceBounds = this.interfaceBounds;
        PhysicalObjectType upperBound = this.upperBound;
        boolean and = false;
        if (upperBound.hasSuperClassType() || interfaceBounds.isEmpty()) {
            upperBound.toFriendlyString(b);
            and = true;
        }
        for (InterfaceObjectType interfaceBound : interfaceBounds) {
            if (and) b.append('&');
            interfaceBound.toFriendlyString(b);
            and = true;
        }
        b.append('>');
        return b;
    }
}

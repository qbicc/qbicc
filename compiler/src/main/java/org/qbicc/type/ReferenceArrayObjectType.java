package org.qbicc.type;

/**
 * An object type whose elements are references to some ObjectType.
 * For any given program, there is a large, but finite, set of possible
 * ReferenceArrayObjectTypes.  However, in qbicc we use a single runtime
 * typeId for all of these logically distinct runtime types and encode
 * the details using "instance fields" containing the typeId of the element
 * ObjectType and the dimension count.
 *
 * To give some concrete examples:
 *  1. String[] is a RefArray with an elementType of String and a dimension count of 1.
 *  2. Iterable[][] is a RefArray with an elementType of Iterable and a dimension count of 2.
 *  3. byte[][][] is a RefArray with an elementType of byte[] and a dimension count of 2.
 *     In effect, we encode this array as-if byte[] was a class.
 */
public final class ReferenceArrayObjectType extends ArrayObjectType {
    private final ObjectType elementType;

    ReferenceArrayObjectType(final TypeSystem typeSystem, final ClassObjectType objectClass, final ObjectType elementType) {
        super(typeSystem, elementType.hashCode(), objectClass);
        this.elementType = elementType;
    }

    public long getSize() throws IllegalStateException {
        return 0;
    }

    public boolean isSubtypeOf(final ObjectType other) {
        return super.isSubtypeOf(other)
            || other instanceof ReferenceArrayObjectType && isSubtypeOf((ReferenceArrayObjectType) other);
    }

    public boolean isSubtypeOf(final ReferenceArrayObjectType other) {
        return this == other
            || elementType.isSubtypeOf(other.elementType);
    }

    public ObjectType getCommonSupertype(final ObjectType other) {
        if (other instanceof ReferenceArrayObjectType) {
            ReferenceType elementType = getElementType();
            ReferenceType otherElementType = ((ReferenceArrayObjectType) other).getElementType();
            ObjectType commonBound = elementType.getUpperBound().getCommonSupertype(otherElementType.getUpperBound());
            return commonBound.getReferenceArrayObject();
        } else {
            return super.getCommonSupertype(other);
        }
    }

    public ReferenceType getElementType() {
        return elementType.getReference();
    }

    public ObjectType getElementObjectType() {
        return elementType;
    }

    public ObjectType getLeafElementType() {
        if (elementType instanceof ReferenceArrayObjectType) {
            return ((ReferenceArrayObjectType) elementType).getLeafElementType();
        } else {
            return elementType;
        }
    }

    public int getDimensionCount() {
        if (elementType instanceof ReferenceArrayObjectType) {
            return 1 + ((ReferenceArrayObjectType) elementType).getDimensionCount();
        } else {
            return 1;
        }
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return elementType.toFriendlyString(b.append("ref_array").append('.'));
    }

    @Override
    public final boolean equals(ObjectType other) {
        return other instanceof ReferenceArrayObjectType && equals((ReferenceArrayObjectType) other);
    }

    public boolean equals(ReferenceArrayObjectType other) {
        return super.equals(other) && elementType.equals(other.elementType);
    }
}

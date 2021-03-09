package cc.quarkus.qcc.type;

/**
 * An object type whose elements are references to some ObjectType.
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
            return commonBound.getReference().getReferenceArrayObject();
        } else {
            return super.getCommonSupertype(other);
        }
    }

    public ReferenceType getElementType() {
        return elementType.getReference();
    }

    public ValueType getLeafElementType() {
        if (elementType instanceof ArrayObjectType) {
            return ((ArrayObjectType) elementType).getLeafElementType();
        } else {
            return elementType;
        }
    }

    public int getDimensionCount() {
        if (elementType instanceof ArrayObjectType) {
            return 1 + ((ArrayObjectType) elementType).getDimensionCount();
        } else {
            return 1;
        }
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return elementType.toFriendlyString(b.append("ref_array").append('.'));
    }
}

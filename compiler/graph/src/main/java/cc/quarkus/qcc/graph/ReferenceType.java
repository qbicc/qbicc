package cc.quarkus.qcc.graph;

/**
 *
 */
public interface ReferenceType extends Type {
    ClassType getUpperBound();

    default boolean hasLowerBound() {
        return getLowerBound() != null;
    }

    ClassType getLowerBound();

    static ReferenceType createExact(ClassType type) {
        return create(type, type);
    }

    static ReferenceType createCovariant(ClassType topType) {
        return create(topType, null);
    }

    default ReferenceType withUpperBound(ClassType upperBound) {
        return create(upperBound, getLowerBound());
    }

    default ReferenceType withLowerBound(ClassType lowerBound) {
        return create(getUpperBound(), lowerBound);
    }

    static ReferenceType create(ClassType upperBound, ClassType lowerBound) {
        return new ReferenceTypeImpl(upperBound, lowerBound);
    }

    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof ReferenceType && isAssignableFrom((ReferenceType) otherType);
    }

    default boolean isAssignableFrom(ReferenceType otherType) {
        ClassType myUpper = getUpperBound();
        ClassType myLower = getLowerBound();
        ClassType otherUpper = otherType.getUpperBound();
        ClassType otherLower = otherType.getLowerBound();
        return myUpper.isSuperTypeOf(otherUpper) && myLower.isSuperTypeOf(otherLower);
    }
}

package cc.quarkus.qcc.graph;

/**
 * A Java array class type.  Java arrays are Java Objects, and so have a "class".  The element type
 * can be a primitive or a {@code ClassType}.
 */
public interface ArrayClassType extends ClassType, ArrayType {
    Type getElementType();

    default boolean isSuperTypeOf(ClassType other) {
        return other == this;
    }

    default boolean isAssignableFrom(ClassType other) {
        return other instanceof ArrayClassType && isAssignableFrom((ArrayClassType) other);
    }

    default boolean isAssignableFrom(ArrayClassType other) {
        Type elementType = getElementType();
        Type otherType = other.getElementType();
        return elementType == otherType || elementType instanceof ClassType && otherType instanceof ClassType && ((ClassType) elementType).isAssignableFrom((ClassType) otherType);
    }

    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof ArrayClassType && isAssignableFrom((ArrayClassType) otherType);
    }
}

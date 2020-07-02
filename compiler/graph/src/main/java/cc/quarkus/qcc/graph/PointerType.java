package cc.quarkus.qcc.graph;

/**
 * A pointer to another type.  The size and behavior of a pointer type may depend on the target platform.
 */
public interface PointerType extends WordType {
    /**
     * Get the type being pointed to.
     *
     * @return the pointee type
     */
    NativeObjectType getPointeeType();

    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof PointerType && getSize() == ((PointerType) otherType).getSize() && getPointeeType().equals(((PointerType) otherType).getPointeeType());
    }
}

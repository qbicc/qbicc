package cc.quarkus.qcc.graph;

/**
 * A pointer to another type.  The size and behavior of a pointer type may depend on the target platform.
 */
public interface PointerType<T extends Type> extends WordType {
    /**
     * Get the type being pointed to.
     *
     * @return the pointee type
     */
    T getPointeeType();

    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof PointerType && getPointeeType().equals(((PointerType<?>) otherType).getPointeeType());
    }
}

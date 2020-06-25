package cc.quarkus.qcc.graph;

/**
 * The type of the {@code null} value.
 */
public interface NullType extends Type {
    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof NullType;
    }
}

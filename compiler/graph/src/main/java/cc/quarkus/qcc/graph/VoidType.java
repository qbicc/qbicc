package cc.quarkus.qcc.graph;

/**
 *
 */
public interface VoidType extends Type {
    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof VoidType;
    }
}

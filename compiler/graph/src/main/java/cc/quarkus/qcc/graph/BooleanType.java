package cc.quarkus.qcc.graph;

/**
 *
 */
public interface BooleanType extends WordType {
    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof BooleanType;
    }
}

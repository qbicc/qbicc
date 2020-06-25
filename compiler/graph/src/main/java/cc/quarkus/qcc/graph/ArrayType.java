package cc.quarkus.qcc.graph;

/**
 *
 */
public interface ArrayType extends Type {
    Type getElementType();

    default boolean isAssignableFrom(Type otherType) {
        return otherType == this;
    }
}

package cc.quarkus.qcc.graph;

/**
 *
 */
public interface SignedIntegerType extends IntegerType {
    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof SignedIntegerType && ((SignedIntegerType) otherType).getSize() == getSize();
    }
}

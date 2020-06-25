package cc.quarkus.qcc.graph;

/**
 *
 */
public interface UnsignedIntegerType extends IntegerType {
    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof UnsignedIntegerType && ((UnsignedIntegerType) otherType).getSize() == getSize();
    }
}

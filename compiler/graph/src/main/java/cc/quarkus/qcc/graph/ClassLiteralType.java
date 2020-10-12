package cc.quarkus.qcc.graph;

/**
 * A type representing a class literal, before it is promoted to an object.
 */
public interface ClassLiteralType extends Type {
    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof ClassLiteralType;
    }

    default Value zero() {
        throw new UnsupportedOperationException();
    }
}

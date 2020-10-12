package cc.quarkus.qcc.graph;

/**
 * A type representing a string literal, before it is promoted to an object.
 */
public interface StringLiteralType extends Type {
    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof StringLiteralType;
    }
}

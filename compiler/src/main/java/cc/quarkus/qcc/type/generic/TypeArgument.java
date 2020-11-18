package cc.quarkus.qcc.type.generic;

/**
 * Any kind of type argument.
 */
public interface TypeArgument {
    default boolean isAny() {
        return false;
    }

    default AnyTypeArgument asAny() {
        throw new ClassCastException();
    }

    default boolean isBound() {
        return false;
    }

    default BoundTypeArgument asBound() {
        throw new ClassCastException();
    }

    StringBuilder toString(StringBuilder b);
}

package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 * The explicit "any" type argument, encoded as {@code *}.
 */
public interface AnyTypeArgument extends TypeArgument {
    AnyTypeArgument INSTANCE = new AnyTypeArgument() {};

    default boolean isAny() {
        return true;
    }

    default AnyTypeArgument asAny() {
        return this;
    }
}

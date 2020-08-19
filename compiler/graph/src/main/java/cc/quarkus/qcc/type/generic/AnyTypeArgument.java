package cc.quarkus.qcc.type.generic;

/**
 * The explicit "any" type argument, encoded as {@code *}.
 */
public final class AnyTypeArgument implements TypeArgument {
    public static final AnyTypeArgument INSTANCE = new AnyTypeArgument();

    private AnyTypeArgument() {
    }

    public boolean isAny() {
        return true;
    }

    public AnyTypeArgument asAny() {
        return this;
    }

    public StringBuilder toString(final StringBuilder b) {
        return b.append('*');
    }
}

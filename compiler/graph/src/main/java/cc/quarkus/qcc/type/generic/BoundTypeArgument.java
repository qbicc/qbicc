package cc.quarkus.qcc.type.generic;

/**
 * A type argument that is a bound of some sort.
 */
public interface BoundTypeArgument extends TypeArgument {
    default boolean isBound() {
        return true;
    }

    default BoundTypeArgument asBound() {
        return this;
    }

    /**
     * Get the variance of the bound.
     *
     * @return the variance of the bound
     */
    Variance getVariance();

    /**
     * Get the type of the bound.
     *
     * @return the type of the bound
     */
    ReferenceTypeSignature getValue();
}

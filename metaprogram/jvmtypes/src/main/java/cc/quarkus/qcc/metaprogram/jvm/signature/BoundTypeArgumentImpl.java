package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 *
 */
final class BoundTypeArgumentImpl implements BoundTypeArgument {
    private final Variance variance;
    private final ReferenceTypeSignature type;

    BoundTypeArgumentImpl(final Variance variance, final ReferenceTypeSignature type) {
        this.variance = variance;
        this.type = type;
    }

    public Variance getVariance() {
        return variance;
    }

    public ReferenceTypeSignature getValue() {
        return type;
    }
}

package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 *
 */
final class BoundTypeArgumentImpl implements ClassTypeSignature.BoundTypeArgument {
    private final ClassTypeSignature.Variance variance;
    private final ReferenceTypeSignature type;

    BoundTypeArgumentImpl(final ClassTypeSignature.Variance variance, final ReferenceTypeSignature type) {
        this.variance = variance;
        this.type = type;
    }

    public ClassTypeSignature.Variance getVariance() {
        return variance;
    }

    public ReferenceTypeSignature getValue() {
        return type;
    }
}

package cc.quarkus.qcc.type.generic;

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

    public StringBuilder toString(final StringBuilder b) {
        if (variance == Variance.CONTRAVARIANT) {
            b.append("super ");
        } else if (variance == Variance.COVARIANT) {
            b.append("extends ");
        } else {
            b.append("exactly ");
        }
        type.toString(b);
        return b;
    }
}

package cc.quarkus.qcc.graph;

/**
 *
 */
public final class Neg extends AbstractUnaryValue {
    Neg(final Value v) {
        super(v);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

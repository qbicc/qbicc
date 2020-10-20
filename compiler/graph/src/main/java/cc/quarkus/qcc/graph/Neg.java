package cc.quarkus.qcc.graph;

/**
 *
 */
public final class Neg extends AbstractUnaryValue {
    Neg(final int line, final int bci, final Value v) {
        super(line, bci, v);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

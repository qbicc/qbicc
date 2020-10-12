package cc.quarkus.qcc.graph;

/**
 *
 */
public final class Xor extends AbstractBinaryValue implements CommutativeBinaryValue {
    Xor(final Value v1, final Value v2) {
        super(v1, v2);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

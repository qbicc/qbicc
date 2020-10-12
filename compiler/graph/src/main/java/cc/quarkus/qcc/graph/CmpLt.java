package cc.quarkus.qcc.graph;

/**
 *
 */
public final class CmpLt extends AbstractCmp implements NonCommutativeBinaryValue {
    CmpLt(final Value v1, final Value v2) {
        super(v1, v2);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

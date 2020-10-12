package cc.quarkus.qcc.graph;

/**
 *
 */
public final class BitCast extends AbstractWordCastValue {
    BitCast(final Value value, final WordType toType) {
        super(value, toType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

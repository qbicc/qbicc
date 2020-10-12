package cc.quarkus.qcc.graph;

/**
 *
 */
public final class Truncate extends AbstractWordCastValue {
    Truncate(final Value value, final WordType toType) {
        super(value, toType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

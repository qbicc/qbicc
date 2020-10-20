package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.WordType;

/**
 *
 */
public final class BitCast extends AbstractWordCastValue {
    BitCast(final int line, final int bci, final Value value, final WordType toType) {
        super(line, bci, value, toType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

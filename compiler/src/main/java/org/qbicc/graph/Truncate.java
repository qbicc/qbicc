package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.WordType;

/**
 *
 */
public final class Truncate extends AbstractWordCastValue {
    Truncate(final ProgramLocatable pl, final Value value, final WordType toType) {
        super(pl, value, toType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    boolean equals(AbstractWordCastValue other) {
        return other instanceof Truncate tr && equals(tr);
    }

    boolean equals(Truncate other) {
        return super.equals(other);
    }

    @Override
    String getNodeName() {
        return "Truncate";
    }
}

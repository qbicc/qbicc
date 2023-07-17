package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.WordType;

/**
 *
 */
public final class BitCast extends AbstractWordCastValue {
    BitCast(final ProgramLocatable pl, final Value value, final WordType toType) {
        super(pl, value, toType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    boolean equals(AbstractWordCastValue other) {
        return other instanceof BitCast bc && equals(bc);
    }

    boolean equals(BitCast other) {
        return super.equals(other);
    }

    @Override
    String getNodeName() {
        return "BitCast";
    }
}

package org.qbicc.graph;

import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class BitCast extends AbstractWordCastValue {
    BitCast(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value value, final WordType toType) {
        super(callSite, element, line, bci, value, toType);
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

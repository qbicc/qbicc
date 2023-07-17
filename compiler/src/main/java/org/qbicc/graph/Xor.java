package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;

/**
 *
 */
public final class Xor extends AbstractBinaryValue implements CommutativeBinaryValue {
    Xor(final ProgramLocatable pl, final Value v1, final Value v2) {
        super(pl, v1, v2);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "Xor";
    }
}

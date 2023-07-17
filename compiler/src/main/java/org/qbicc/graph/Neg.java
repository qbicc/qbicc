package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;

/**
 *
 */
public final class Neg extends AbstractUnaryValue {
    Neg(final ProgramLocatable pl, final Value v) {
        super(pl, v);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "Neg";
    }
}

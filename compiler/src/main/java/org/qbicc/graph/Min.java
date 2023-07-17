package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;

/**
 *
 */
public final class Min extends AbstractBinaryValue implements CommutativeBinaryValue {
    Min(final ProgramLocatable pl, final Value v1, final Value v2) {
        super(pl, v1, v2);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean isDefLe(Value other) {
        return other.equals(getLeftInput()) || other.equals(getRightInput()) || super.isDefLe(other);
    }

    @Override
    String getNodeName() {
        return "Min";
    }
}

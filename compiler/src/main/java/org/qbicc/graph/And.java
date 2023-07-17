package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.BooleanType;

/**
 *
 */
public final class And extends AbstractBinaryValue implements CommutativeBinaryValue {
    And(final ProgramLocatable pl, final Value v1, final Value v2) {
        super(pl, v1, v2);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public Value getValueIfTrue(BasicBlockBuilder bbb, Value input) {
        assert getType() instanceof BooleanType;
        // both inputs must be true
        // TODO: merge values algorithm
        return getLeftInput().getValueIfTrue(bbb, getRightInput().getValueIfTrue(bbb, input));
    }

    @Override
    String getNodeName() {
        return "And";
    }
}

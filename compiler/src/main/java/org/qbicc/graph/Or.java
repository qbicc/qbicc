package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.BooleanType;

/**
 *
 */
public final class Or extends AbstractBinaryValue implements CommutativeBinaryValue {
    Or(final ProgramLocatable pl, final Value v1, final Value v2) {
        super(pl, v1, v2);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public Value getValueIfFalse(BasicBlockBuilder bbb, Value input) {
        assert getType() instanceof BooleanType;
        // both inputs must be false
        // TODO: merge values algorithm
        return getLeftInput().getValueIfFalse(bbb, getRightInput().getValueIfFalse(bbb, input));
    }

    @Override
    String getNodeName() {
        return "Or";
    }
}

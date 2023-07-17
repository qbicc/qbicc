package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.literal.Literal;
import org.qbicc.type.BooleanType;

/**
 *
 */
public final class IsEq extends AbstractBooleanCompare implements CommutativeBinaryValue {
    IsEq(final ProgramLocatable pl, final Value v1, final Value v2, final BooleanType booleanType) {
        super(pl, v1, v2, booleanType);
    }

    @Override
    public Value getValueIfTrue(BasicBlockBuilder bbb, Value input) {
        if (input.equals(getLeftInput()) && getRightInput() instanceof Literal) {
            return getRightInput();
        } else if (input.equals(getRightInput()) && getLeftInput() instanceof Literal) {
            return getLeftInput();
        } else {
            return super.getValueIfTrue(bbb, input);
        }
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "IsEq";
    }
}

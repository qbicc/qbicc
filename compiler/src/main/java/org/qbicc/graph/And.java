package org.qbicc.graph;

import org.qbicc.type.BooleanType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class And extends AbstractBinaryValue implements CommutativeBinaryValue {
    And(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value v1, final Value v2) {
        super(callSite, element, line, bci, v1, v2);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
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

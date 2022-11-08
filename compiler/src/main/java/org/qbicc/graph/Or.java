package org.qbicc.graph;

import org.qbicc.type.BooleanType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class Or extends AbstractBinaryValue implements CommutativeBinaryValue {
    Or(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value v1, final Value v2) {
        super(callSite, element, line, bci, v1, v2);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
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

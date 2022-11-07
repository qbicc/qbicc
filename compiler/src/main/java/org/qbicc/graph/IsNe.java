package org.qbicc.graph;

import org.qbicc.graph.literal.Literal;
import org.qbicc.type.BooleanType;
import org.qbicc.type.NullableType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class IsNe extends AbstractBooleanCompare implements CommutativeBinaryValue {
    IsNe(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value v1, final Value v2, final BooleanType booleanType) {
        super(callSite, element, line, bci, v1, v2, booleanType);
    }

    @Override
    public Value getValueIfTrue(BasicBlockBuilder bbb, Value input) {
        if (input.getType() instanceof NullableType) {
            if (input.equals(getLeftInput()) && getRightInput() instanceof Literal ll && ll.isZero() ||
                input.equals(getRightInput()) && getLeftInput() instanceof Literal rl && rl.isZero()) {
                return bbb.notNull(input);
            }
        }
        return super.getValueIfTrue(bbb, input);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "IsNe";
    }
}

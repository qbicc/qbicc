package org.qbicc.graph;

import org.qbicc.type.BooleanType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class IsLt extends AbstractBooleanCompare implements NonCommutativeBinaryValue {
    IsLt(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value v1, final Value v2, final BooleanType booleanType) {
        super(callSite, element, line, bci, v1, v2, booleanType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "IsLt";
    }
}

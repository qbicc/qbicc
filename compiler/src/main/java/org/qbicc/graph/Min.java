package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class Min extends AbstractBinaryValue implements CommutativeBinaryValue {
    Min(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value v1, final Value v2) {
        super(callSite, element, line, bci, v1, v2);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean isDefLe(Value other) {
        return other.equals(getLeftInput()) || other.equals(getRightInput()) || super.isDefLe(other);
    }
}

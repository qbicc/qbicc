package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A value that asserts non-nullity.
 */
public final class NotNull extends AbstractUnaryValue {
    NotNull(Node callSite, ExecutableElement element, int line, int bci, Value input) {
        super(callSite, element, line, bci, input);
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "NotNull";
    }
}

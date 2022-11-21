package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class Neg extends AbstractUnaryValue {
    Neg(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value v) {
        super(callSite, element, line, bci, v);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "Neg";
    }
}

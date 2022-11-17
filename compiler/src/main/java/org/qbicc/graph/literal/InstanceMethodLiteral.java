package org.qbicc.graph.literal;

import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InstanceMethodElement;

/**
 *
 */
public final class InstanceMethodLiteral extends MethodLiteral {
    InstanceMethodLiteral(InstanceMethodElement element) {
        super(element);
    }

    InstanceMethodLiteral(ExecutableElement element) {
        this((InstanceMethodElement) element);
    }

    @Override
    public InstanceMethodElement getElement() {
        return (InstanceMethodElement) super.getElement();
    }

    @Override
    public InstanceMethodType getPointeeType() {
        return getElement().getType();
    }

    @Override
    public boolean equals(MethodLiteral other) {
        return other instanceof InstanceMethodLiteral iml && equals(iml);
    }

    public boolean equals(InstanceMethodLiteral other) {
        return super.equals(other);
    }

    @Override
    public <T, R> R accept(LiteralVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

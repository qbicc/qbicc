package org.qbicc.graph.literal;

import org.qbicc.type.StaticMethodType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.StaticMethodElement;

/**
 *
 */
public final class StaticMethodLiteral extends MethodLiteral {
    StaticMethodLiteral(StaticMethodElement element) {
        super(element);
    }

    StaticMethodLiteral(ExecutableElement element) {
        this((StaticMethodElement) element);
    }

    @Override
    public StaticMethodElement getElement() {
        return (StaticMethodElement) super.getElement();
    }

    @Override
    public StaticMethodType getPointeeType() {
        return getElement().getType();
    }

    @Override
    public boolean equals(MethodLiteral other) {
        return other instanceof StaticMethodLiteral sml && equals(sml);
    }

    public boolean equals(StaticMethodLiteral other) {
        return super.equals(other);
    }

    @Override
    public <T, R> R accept(LiteralVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

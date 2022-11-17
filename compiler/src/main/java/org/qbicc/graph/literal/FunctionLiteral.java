package org.qbicc.graph.literal;

import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;

/**
 *
 */
public final class FunctionLiteral extends InvokableLiteral {
    FunctionLiteral(FunctionElement element) {
        super(element);
    }

    FunctionLiteral(ExecutableElement element) {
        this((FunctionElement) element);
    }

    @Override
    public FunctionElement getElement() {
        return (FunctionElement) super.getElement();
    }

    @Override
    public boolean equals(InvokableLiteral other) {
        return other instanceof FunctionLiteral cl && equals(cl);
    }

    public boolean equals(FunctionLiteral other) {
        return super.equals(other);
    }

    @Override
    public StringBuilder toReferenceString(StringBuilder b) {
        FunctionElement element = getElement();
        return b.append('@').append(element.getName());
    }

    @Override
    public <T, R> R accept(LiteralVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

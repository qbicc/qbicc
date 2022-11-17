package org.qbicc.graph.literal;

import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class ConstructorLiteral extends InvokableLiteral {
    ConstructorLiteral(ConstructorElement element) {
        super(element);
    }

    ConstructorLiteral(ExecutableElement element) {
        this((ConstructorElement) element);
    }

    @Override
    public ConstructorElement getExecutable() {
        return (ConstructorElement) super.getExecutable();
    }

    @Override
    public InstanceMethodType getPointeeType() {
        return getExecutable().getType();
    }

    @Override
    public boolean equals(InvokableLiteral other) {
        return other instanceof ConstructorLiteral cl && equals(cl);
    }

    public boolean equals(ConstructorLiteral other) {
        return super.equals(other);
    }

    @Override
    public StringBuilder toReferenceString(StringBuilder b) {
        ConstructorElement element = getExecutable();
        String niceClass = element.getEnclosingType().getInternalName().replace('/', '.');
        return element.getDescriptor().toString(b.append('@').append(niceClass).append('#').append("<init>"));
    }

    @Override
    public <T, R> R accept(LiteralVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

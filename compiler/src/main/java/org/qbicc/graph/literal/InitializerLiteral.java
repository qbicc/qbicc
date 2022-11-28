package org.qbicc.graph.literal;

import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InitializerElement;

/**
 *
 */
public final class InitializerLiteral extends ExecutableLiteral {
    InitializerLiteral(InitializerElement element) {
        super(element);
    }

    InitializerLiteral(final ExecutableElement element) {
        this((InitializerElement) element);
    }

    @Override
    public InitializerElement getExecutable() {
        return (InitializerElement) super.getExecutable();
    }

    @Override
    public boolean equals(ExecutableLiteral other) {
        return other instanceof InitializerLiteral il && equals(il);
    }

    public boolean equals(InitializerLiteral other) {
        return super.equals(other);
    }

    @Override
    public StringBuilder toReferenceString(StringBuilder b) {
        InitializerElement element = getExecutable();
        String niceClass = element.getEnclosingType().getInternalName().replace('/', '.');
        return b.append('@').append(niceClass).append('#').append("<clinit>");
    }

    @Override
    public <T, R> R accept(LiteralVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

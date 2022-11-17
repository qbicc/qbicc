package org.qbicc.graph.literal;

import org.qbicc.type.MethodType;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public abstract class MethodLiteral extends InvokableLiteral {
    MethodLiteral(MethodElement element) {
        super(element);
    }

    @Override
    public MethodElement getElement() {
        return (MethodElement) super.getElement();
    }

    @Override
    public MethodType getPointeeType() {
        return getElement().getType();
    }

    @Override
    public final boolean equals(InvokableLiteral other) {
        return other instanceof MethodLiteral ml && equals(ml);
    }

    public boolean equals(MethodLiteral other) {
        return super.equals(other);
    }

    @Override
    public StringBuilder toReferenceString(StringBuilder b) {
        MethodElement element = getElement();
        String niceClass = element.getEnclosingType().getInternalName().replace('/', '.');
        return element.getDescriptor().toString(b.append('@').append(niceClass).append('#').append(element.getName()));
    }
}

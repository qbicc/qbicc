package org.qbicc.graph.literal;

import org.qbicc.type.MethodType;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public abstract sealed class MethodLiteral extends InvokableLiteral permits InstanceMethodLiteral, StaticMethodLiteral {
    MethodLiteral(MethodElement element) {
        super(element);
    }

    @Override
    public MethodElement getExecutable() {
        return (MethodElement) super.getExecutable();
    }

    @Override
    public MethodType getPointeeType() {
        return getExecutable().getType();
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
        MethodElement element = getExecutable();
        String niceClass = element.getEnclosingType().getInternalName().replace('/', '.');
        return element.getDescriptor().toString(b.append('@').append(niceClass).append('#').append(element.getName()));
    }
}

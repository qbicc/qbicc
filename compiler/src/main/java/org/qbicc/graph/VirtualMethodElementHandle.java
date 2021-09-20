package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 * A handle for an instance virtual method.
 */
public final class VirtualMethodElementHandle extends InstanceMethodElementHandle {

    VirtualMethodElementHandle(ExecutableElement element, int line, int bci, MethodElement methodElement, Value instance) {
        super(element, line, bci, methodElement, instance);
        if (methodElement.isStatic() || methodElement.getEnclosingType().isInterface()) {
            throw new IllegalArgumentException("Wrong argument kind for virtual method handle");
        }
    }

    int calcHashCode() {
        return super.calcHashCode() * 19 + VirtualMethodElementHandle.class.hashCode();
    }

    @Override
    public boolean equals(InstanceMethodElementHandle other) {
        return other instanceof VirtualMethodElementHandle && equals((VirtualMethodElementHandle) other);
    }

    public boolean equals(final VirtualMethodElementHandle other) {
        return super.equals(other);
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public <T> long accept(ValueHandleVisitorLong<T> visitor, T param) {
        return visitor.visit(param, this);
    }
}

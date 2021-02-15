package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 * A handle for an instance interface method.
 */
public final class InterfaceMethodElementHandle extends InstanceMethodElementHandle {
    InterfaceMethodElementHandle(ExecutableElement element, int line, int bci, MethodElement methodElement, Value instance) {
        super(element, line, bci, methodElement, instance);
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

    public <T> long accept(final ValueHandleVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

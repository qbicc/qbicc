package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 * A handle for an instance exact method.
 */
public final class ExactMethodElementHandle extends InstanceMethodElementHandle {

    ExactMethodElementHandle(ExecutableElement element, int line, int bci, MethodElement methodElement, Value instance) {
        super(element, line, bci, methodElement, instance);
    }

    int calcHashCode() {
        return super.calcHashCode() * 19 + ExactMethodElementHandle.class.hashCode();
    }

    @Override
    public boolean equals(InstanceMethodElementHandle other) {
        return other instanceof ExactMethodElementHandle && equals((ExactMethodElementHandle) other);
    }

    public boolean equals(final ExactMethodElementHandle other) {
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

package org.qbicc.graph;

import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 * A handle for an instance exact method.
 */
public final class ExactMethodElementHandle extends InstanceMethodElementHandle {

    ExactMethodElementHandle(ExecutableElement element, int line, int bci, MethodElement methodElement, Value instance, MethodDescriptor callSiteDescriptor, InstanceMethodType callSiteType) {
        super(element, line, bci, methodElement, instance, callSiteDescriptor, callSiteType);
    }

    int calcHashCode() {
        return super.calcHashCode() * 19 + ExactMethodElementHandle.class.hashCode();
    }

    @Override
    String getNodeName() {
        return "ExactMethod";
    }

    @Override
    public boolean equals(InstanceMethodElementHandle other) {
        return other instanceof ExactMethodElementHandle && equals((ExactMethodElementHandle) other);
    }

    public boolean equals(final ExactMethodElementHandle other) {
        return super.equals(other);
    }

    public boolean isConstantLocation() {
        return true;
    }

    @Override
    public <T, R> R accept(PointerValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final PointerValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

package org.qbicc.graph;

import org.qbicc.type.StaticMethodType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 * A handle for a static method.
 */
public final class StaticMethodElementHandle extends Executable {

    StaticMethodElementHandle(ExecutableElement element, int line, int bci, MethodElement methodElement, MethodDescriptor callSiteDescriptor, StaticMethodType callSiteType) {
        super(element, line, bci, methodElement, callSiteDescriptor, callSiteType);
    }

    @Override
    public MethodElement getExecutable() {
        return (MethodElement) super.getExecutable();
    }

    @Override
    public StaticMethodType getPointeeType() {
        return (StaticMethodType) super.getPointeeType();
    }

    @Override
    public StaticMethodType getCallSiteType() {
        return (StaticMethodType) super.getCallSiteType();
    }

    public boolean equals(final Executable other) {
        return other instanceof StaticMethodElementHandle && equals((StaticMethodElementHandle) other);
    }

    public boolean equals(final StaticMethodElementHandle other) {
        return super.equals(other);
    }

    public boolean isConstantLocation() {
        return true;
    }

    @Override
    public boolean isValueConstant() {
        return true;
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueHandleVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "StaticMethod";
    }
}

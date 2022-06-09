package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InstanceMethodElement;

/**
 * A lookup of an instance method using a virtual method table (vtable).
 */
public final class VirtualMethodLookup extends MethodLookup {
    VirtualMethodLookup(Node callSite, ExecutableElement element, int line, int bci, InstanceMethodElement method, Value instanceTypeId) {
        super(callSite, element, line, bci, method, instanceTypeId);
    }

    @Override
    String getNodeName() {
        return "VirtualMethodLookup";
    }

    @Override
    int calcHashCode() {
        return VirtualMethodLookup.class.hashCode() * 19 + super.calcHashCode();
    }

    @Override
    public boolean equals(MethodLookup other) {
        return other instanceof VirtualMethodLookup vml && equals(vml);
    }

    public boolean equals(VirtualMethodLookup other) {
        return super.equals(other);
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public <T> long accept(ValueVisitorLong<T> visitor, T param) {
        return visitor.visit(param, this);
    }
}

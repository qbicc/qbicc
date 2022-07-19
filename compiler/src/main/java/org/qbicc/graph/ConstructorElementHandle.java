package org.qbicc.graph;

import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 * A handle for an instance constructor.
 */
public final class ConstructorElementHandle extends Executable {
    private final Value instance;

    ConstructorElementHandle(ExecutableElement element, int line, int bci, ConstructorElement constructorElement, Value instance, MethodDescriptor callSiteDescriptor, InstanceMethodType callSiteType) {
        super(element, line, bci, constructorElement, callSiteDescriptor, callSiteType);
        this.instance = instance;
    }

    public ValueType getInstanceType() {
        return instance.getType();
    }

    public Value getInstance() {
        return instance;
    }

    @Override
    public ConstructorElement getExecutable() {
        return (ConstructorElement) super.getExecutable();
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? instance : Util.throwIndexOutOfBounds(index);
    }

    int calcHashCode() {
        return super.calcHashCode() * 19 + instance.hashCode();
    }

    @Override
    String getNodeName() {
        return "Constructor";
    }

    @Override
    public InstanceMethodType getPointeeType() {
        return (InstanceMethodType) super.getPointeeType();
    }

    @Override
    public InstanceMethodType getCallSiteType() {
        return (InstanceMethodType) super.getCallSiteType();
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        instance.toString(b);
        b.append(')');
        return b;
    }

    public boolean equals(final Executable other) {
        return other instanceof ConstructorElementHandle && equals((ConstructorElementHandle) other);
    }

    public boolean equals(final ConstructorElementHandle other) {
        return super.equals(other) && instance.equals(other.instance);
    }

    public boolean isConstantLocation() {
        return true;
    }

    @Override
    public boolean isValueConstant() {
        return true;
    }

    @Override
    public <T, R> R accept(PointerValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public <T> long accept(PointerValueVisitorLong<T> visitor, T param) {
        return visitor.visit(param, this);
    }
}

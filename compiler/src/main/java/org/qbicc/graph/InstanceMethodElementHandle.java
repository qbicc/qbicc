package org.qbicc.graph;

import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 *
 */
public abstract class InstanceMethodElementHandle extends Executable {
    private final Value instance;

    public InstanceMethodElementHandle(final ExecutableElement currentElement, final int line, final int bci, final ExecutableElement executable, Value instance, MethodDescriptor callSiteDescriptor, InstanceMethodType callSiteType) {
        super(currentElement, line, bci, executable, callSiteDescriptor, callSiteType);
        this.instance = instance;
    }

    @Override
    public MethodElement getExecutable() {
        return (MethodElement) super.getExecutable();
    }

    public Value getInstance() {
        return instance;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? instance : Util.throwIndexOutOfBounds(index);
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
    int calcHashCode() {
        return super.calcHashCode() * 19 + instance.hashCode();
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        instance.toReferenceString(b);
        b.append(')');
        return b;
    }

    public final boolean equals(final Executable other) {
        return other instanceof InstanceMethodElementHandle && equals((InstanceMethodElementHandle) other);
    }

    public boolean equals(final InstanceMethodElementHandle other) {
        return super.equals(this) && instance.equals(other.instance);
    }

    @Override
    public boolean isConstantLocation() {
        return false;
    }

    public boolean isValueConstant() {
        return instance.isConstant();
    }
}

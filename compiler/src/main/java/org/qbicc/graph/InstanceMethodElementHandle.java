package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public abstract class InstanceMethodElementHandle extends Executable {
    private final Value instance;

    public InstanceMethodElementHandle(final ExecutableElement currentElement, final int line, final int bci, final ExecutableElement executable, Value instance) {
        super(currentElement, line, bci, executable);
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
    int calcHashCode() {
        return super.calcHashCode() * 19 + instance.hashCode();
    }

    public final boolean equals(final Executable other) {
        return other instanceof InstanceMethodElementHandle && equals((InstanceMethodElementHandle) other);
    }

    public boolean equals(final InstanceMethodElementHandle other) {
        return super.equals(this) && instance.equals(other.instance);
    }
}

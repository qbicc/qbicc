package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public class Store extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;
    private final ValueHandle handle;
    private final Value value;
    private MemoryAtomicityMode mode;

    Store(Node callSite, ExecutableElement element, int line, int bci, Node dependency, ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.handle = handle;
        this.value = value;
        this.mode = mode;
        if (! handle.isWritable()) {
            throw new IllegalArgumentException("Handle is not writable");
        }
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public Value getValue() {
        return value;
    }

    public MemoryAtomicityMode getMode() {
        return mode;
    }

    public void setMode(MemoryAtomicityMode mode) {
        this.mode = mode;
    }

    int calcHashCode() {
        return Objects.hash(dependency, handle, mode);
    }

    public boolean equals(final Object other) {
        return other instanceof Store && equals((Store) other);
    }

    public boolean equals(final Store other) {
        return this == other || other != null && dependency.equals(other.dependency) && handle.equals(other.handle) && value.equals(other.value) && mode == other.mode;
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    @Override
    public ValueHandle getValueHandle() {
        return handle;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? value : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public <T, R> R accept(ActionVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

package org.qbicc.graph;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A load from memory.
 */
public class Load extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final ValueHandle handle;
    private MemoryAtomicityMode mode;

    Load(Node callSite, ExecutableElement element, int line, int bci, Node dependency, ValueHandle handle, MemoryAtomicityMode mode) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.handle = handle;
        this.mode = mode;
        if (! handle.isReadable()) {
            throw new IllegalArgumentException("Handle is not readable");
        }
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    int calcHashCode() {
        return Objects.hash(dependency, handle, mode);
    }

    public boolean equals(final Object other) {
        return other instanceof Load && equals((Load) other);
    }

    public boolean equals(final Load other) {
        return this == other || other != null && dependency.equals(other.dependency) && handle.equals(other.handle) && mode == other.mode;
    }

    public ValueType getType() {
        return handle.getValueType();
    }

    public MemoryAtomicityMode getMode() {
        return mode;
    }

    public void setMode(MemoryAtomicityMode mode) {
        this.mode = mode;
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    @Override
    public ValueHandle getValueHandle() {
        return handle;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

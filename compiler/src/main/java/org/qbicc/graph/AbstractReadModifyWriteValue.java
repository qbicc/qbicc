package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

abstract class AbstractReadModifyWriteValue extends AbstractValue implements ReadModifyWriteValue, OrderedNode {
    private final Node dependency;
    private final ValueHandle target;
    private final Value updateValue;
    private final MemoryAtomicityMode atomicityMode;

    AbstractReadModifyWriteValue(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final ValueHandle target, final Value updateValue, final MemoryAtomicityMode atomicityMode) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.target = target;
        this.updateValue = updateValue;
        this.atomicityMode = atomicityMode;
        if (! target.isWritable()) {
            throw new IllegalArgumentException("Handle is not writable");
        }
        if (! target.isReadable()) {
            throw new IllegalArgumentException("Handle is not readable");
        }
    }

    public ValueType getType() {
        return updateValue.getType();
    }

    public Node getDependency() {
        return dependency;
    }

    public ValueHandle getValueHandle() {
        return target;
    }

    public Value getUpdateValue() {
        return updateValue;
    }

    public MemoryAtomicityMode getAtomicityMode() {
        return atomicityMode;
    }

    int calcHashCode() {
        return Objects.hash(getClass(), dependency, target, updateValue, atomicityMode);
    }

    public boolean equals(final Object other) {
        return other instanceof AbstractReadModifyWriteValue && equals((AbstractReadModifyWriteValue) other);
    }

    private boolean equals(final AbstractReadModifyWriteValue other) {
        return this == other || other.getClass() == getClass() && dependency.equals(other.dependency) && target.equals(other.target)
            && updateValue.equals(other.updateValue) && atomicityMode == other.atomicityMode;
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? updateValue : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }
}

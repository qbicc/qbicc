package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

abstract class AbstractReadModifyWriteValue extends AbstractValue implements ReadModifyWriteValue, OrderedNode {
    private final Node dependency;
    private final ValueHandle target;
    private final Value updateValue;
    private final ReadAccessMode readMode;
    private final WriteAccessMode writeMode;

    AbstractReadModifyWriteValue(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final ValueHandle target, final Value updateValue, ReadAccessMode readMode, WriteAccessMode writeMode) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.target = target;
        this.updateValue = updateValue;
        this.readMode = readMode;
        this.writeMode = writeMode;
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

    public boolean isConstant() {
        // we could possibly make this more exact in the future
        return false;
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

    @Override
    public ReadAccessMode getReadAccessMode() {
        return readMode;
    }

    @Override
    public WriteAccessMode getWriteAccessMode() {
        return writeMode;
    }

    public MemoryAtomicityMode getAtomicityMode() {
        return null;
    }

    int calcHashCode() {
        return Objects.hash(getClass(), dependency, target, updateValue, readMode, writeMode);
    }

    public boolean equals(final Object other) {
        return other instanceof AbstractReadModifyWriteValue && equals((AbstractReadModifyWriteValue) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        b.append(updateValue);
        b.append(',');
        b.append(readMode);
        b.append(',');
        b.append(writeMode);
        b.append(')');
        return b;
    }

    private boolean equals(final AbstractReadModifyWriteValue other) {
        return this == other || other.getClass() == getClass() && dependency.equals(other.dependency) && target.equals(other.target)
            && updateValue.equals(other.updateValue) && readMode.equals(other.readMode) && writeMode.equals(other.writeMode);
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

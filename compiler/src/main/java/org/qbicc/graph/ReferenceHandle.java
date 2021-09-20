package org.qbicc.graph;

import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A value handle for the target of an object reference value.
 */
public final class ReferenceHandle extends AbstractValueHandle {
    private final Value referenceValue;
    private final PointerType pointerType;

    ReferenceHandle(Node callSite, ExecutableElement element, int line, int bci, Value referenceValue) {
        super(callSite, element, line, bci);
        this.referenceValue = referenceValue;
        pointerType = ((ReferenceType) referenceValue.getType()).getUpperBound().getPointer().asCollected();
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    public PointerType getPointerType() {
        return pointerType;
    }

    @Override
    public ObjectType getValueType() {
        return (ObjectType) super.getValueType();
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? referenceValue : Util.throwIndexOutOfBounds(index);
    }

    public Value getReferenceValue() {
        return referenceValue;
    }

    int calcHashCode() {
        return referenceValue.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ReferenceHandle && equals((ReferenceHandle) other);
    }

    public boolean equals(ReferenceHandle other) {
        return this == other || other != null && referenceValue.equals(other.referenceValue);
    }

    /**
     * Reference handles are not writable.
     *
     * @return {@code false} always
     */
    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public MemoryAtomicityMode getDetectedMode() {
        return MemoryAtomicityMode.UNORDERED;
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueHandleVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

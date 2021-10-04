package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A handle for an array element.  The input handle must be a handle to an array or pointer.
 */
public final class ElementOf extends AbstractValueHandle {
    private final ValueHandle inputHandle;
    private final Value index;
    private final PointerType pointerType;

    ElementOf(Node callSite, ExecutableElement element, int line, int bci, ValueHandle inputHandle, Value index) {
        super(callSite, element, line, bci);
        this.inputHandle = inputHandle;
        this.index = index;

        ValueType inputType = inputHandle.getValueType();
        PointerType pointerType;

        if (inputHandle instanceof PointerHandle) {
            pointerType = inputType.getPointer();
        } else if (inputType instanceof ArrayType) {
            pointerType = ((ArrayType) inputType).getElementType().getPointer();
        } else if (inputType instanceof ArrayObjectType) {
            pointerType = ((ArrayObjectType) inputType).getElementType().getPointer();
        } else {
            throw new IllegalArgumentException("Invalid input type: " + inputType);
        }

        this.pointerType = pointerType.withQualifiersFrom(inputHandle.getPointerType());
    }

    @Override
    public PointerType getPointerType() {
        return pointerType;
    }

    public boolean isConstantLocation() {
        return index.isConstant() && inputHandle.isConstantLocation();
    }

    @Override
    public boolean isValueConstant() {
        return index.isConstant() && inputHandle.isValueConstant();
    }

    public Value getIndex() {
        return index;
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    @Override
    public ValueHandle getValueHandle() {
        return inputHandle;
    }

    @Override
    public MemoryAtomicityMode getDetectedMode() {
        return inputHandle.getDetectedMode();
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? this.index : Util.throwIndexOutOfBounds(index);
    }

    int calcHashCode() {
        return Objects.hash(inputHandle, index);
    }

    public boolean equals(final Object other) {
        return other instanceof ElementOf && equals((ElementOf) other);
    }

    public boolean equals(final ElementOf other) {
        return this == other || other != null && inputHandle.equals(other.inputHandle) && index.equals(other.index);
    }

    public <T, R> R accept(final ValueHandleVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueHandleVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

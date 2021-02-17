package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 * A handle for an array element.  The input handle must be a handle to an array or pointer.
 */
public final class ElementOf extends AbstractValueHandle {
    private final ValueHandle inputHandle;
    private final Value index;
    private final ValueType valueType;

    ElementOf(Node callSite, ExecutableElement element, int line, int bci, ValueHandle inputHandle, Value index) {
        super(callSite, element, line, bci);
        this.inputHandle = inputHandle;
        this.index = index;
        ValueType inputType = inputHandle.getValueType();
        if (inputHandle instanceof PointerHandle) {
            valueType = inputHandle.getValueType();
        } else if (inputType instanceof ArrayType) {
            valueType = ((ArrayType) inputType).getElementType();
        } else if (inputType instanceof ArrayObjectType) {
            valueType = ((ArrayObjectType) inputType).getElementType();
        } else {
            throw new IllegalArgumentException("Invalid input type: " + inputType);
        }
    }

    @Override
    public ValueType getValueType() {
        return valueType;
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
}

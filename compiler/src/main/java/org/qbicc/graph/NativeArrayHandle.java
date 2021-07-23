package org.qbicc.graph;

import org.qbicc.type.ArrayType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

import java.lang.annotation.Native;

public class NativeArrayHandle extends AbstractValueHandle {
    private final Value value;

    NativeArrayHandle(Node callSite, ExecutableElement element, int line, int bci, Value value) {
        super(callSite, element, line, bci);
        this.value = value;
    }

    @Override
    int calcHashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof NativeArrayHandle && equals((NativeArrayHandle)other);
    }

    public boolean equals(NativeArrayHandle other) {
        return other == this || other != null && other.value.equals(value);
    }

    @Override
    public PointerType getPointerType() {
        return getValueType().getPointer();
    }

    @Override
    public ArrayType getValueType() {
        return (ArrayType) value.getType();
    }

    public Value getArrayValue() {
        return value;
    }

    @Override
    public MemoryAtomicityMode getDetectedMode() {
        return MemoryAtomicityMode.NONE;
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

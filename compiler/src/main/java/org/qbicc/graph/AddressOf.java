package org.qbicc.graph;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * Get the address of an object referred to by a value handle.
 */
public final class AddressOf extends AbstractValue {
    private final ValueHandle valueHandle;

    AddressOf(Node callSite, ExecutableElement element, int line, int bci, ValueHandle valueHandle) {
        super(callSite, element, line, bci);
        this.valueHandle = valueHandle;
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    @Override
    public ValueHandle getValueHandle() {
        return valueHandle;
    }

    @Override
    int calcHashCode() {
        return valueHandle.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AddressOf && equals((AddressOf) other);
    }

    public boolean equals(AddressOf other) {
        return this == other || other != null && valueHandle.equals(other.valueHandle);
    }

    @Override
    public ValueType getType() {
        return valueHandle.getPointerType();
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

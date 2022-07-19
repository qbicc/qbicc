package org.qbicc.graph;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * Get the address of an object referred to by a value handle.
 */
public final class AddressOf extends AbstractValue {
    private final PointerValue pointerValue;

    AddressOf(Node callSite, ExecutableElement element, int line, int bci, PointerValue pointerValue) {
        super(callSite, element, line, bci);
        this.pointerValue = pointerValue;
    }

    @Override
    public boolean hasPointerValueDependency() {
        return true;
    }

    @Override
    public PointerValue getPointerValue() {
        return pointerValue;
    }

    @Override
    int calcHashCode() {
        return pointerValue.hashCode();
    }

    @Override
    String getNodeName() {
        return "AddressOf";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AddressOf && equals((AddressOf) other);
    }

    public boolean equals(AddressOf other) {
        return this == other || other != null && pointerValue.equals(other.pointerValue);
    }

    @Override
    public ValueType getType() {
        return pointerValue.getType();
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean isConstant() {
        return pointerValue.isConstantLocation();
    }
}

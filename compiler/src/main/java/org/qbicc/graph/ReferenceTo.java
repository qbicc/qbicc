package org.qbicc.graph;

import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * Get the reference of an object referred to by a value handle.
 */
public final class ReferenceTo extends AbstractValue {
    private final ValueHandle valueHandle;
    private final ReferenceType referenceType;

    ReferenceTo(Node callSite, ExecutableElement element, int line, int bci, ValueHandle valueHandle) {
        super(callSite, element, line, bci);
        this.valueHandle = valueHandle;
        ObjectType objectType = (ObjectType) valueHandle.getValueType();
        referenceType = objectType.getReference();
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
    String getNodeName() {
        return "ReferenceTo";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ReferenceTo && equals((ReferenceTo) other);
    }

    public boolean equals(ReferenceTo other) {
        return this == other || other != null && valueHandle.equals(other.valueHandle);
    }

    @Override
    public ReferenceType getType() {
        return referenceType;
    }

    public boolean isNullable() {
        return false;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

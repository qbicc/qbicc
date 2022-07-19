package org.qbicc.graph;

import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * Get the reference of an object referred to by a value handle.
 */
public final class ReferenceTo extends AbstractValue {
    private final PointerValue pointerValue;
    private final ReferenceType referenceType;

    ReferenceTo(Node callSite, ExecutableElement element, int line, int bci, PointerValue pointerValue) {
        super(callSite, element, line, bci);
        this.pointerValue = pointerValue;
        ObjectType objectType = (ObjectType) pointerValue.getPointeeType();
        referenceType = objectType.getReference();
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
        return "ReferenceTo";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ReferenceTo && equals((ReferenceTo) other);
    }

    public boolean equals(ReferenceTo other) {
        return this == other || other != null && pointerValue.equals(other.pointerValue);
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

    @Override
    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

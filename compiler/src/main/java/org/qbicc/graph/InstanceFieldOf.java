package org.qbicc.graph;

import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InstanceFieldElement;

/**
 * A field handle for an instance field.
 */
public final class InstanceFieldOf extends Field {
    private final ValueHandle instance;
    private final PhysicalObjectType instanceType;

    InstanceFieldOf(ExecutableElement element, int line, int bci, InstanceFieldElement fieldElement, ValueType valueType, ValueHandle instance) {
        super(element, line, bci, fieldElement, valueType.getPointer().withQualifiersFrom(instance.getPointerType()));
        instanceType = (PhysicalObjectType) instance.getValueType();
        this.instance = instance;
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    @Override
    public ValueHandle getValueHandle() {
        return instance;
    }

    @Override
    public InstanceFieldElement getVariableElement() {
        return (InstanceFieldElement) super.getVariableElement();
    }

    public PhysicalObjectType getInstanceType() {
        return instanceType;
    }

    int calcHashCode() {
        return super.calcHashCode() * 19 + instance.hashCode();
    }

    @Override
    String getNodeName() {
        return "InstanceFieldOf";
    }

    public boolean equals(final Object other) {
        return other instanceof InstanceFieldOf ifo && equals(ifo);
    }

    public boolean equals(final InstanceFieldOf other) {
        return this == other || other != null && getVariableElement().equals(other.getVariableElement()) && instance.equals(other.instance);
    }

    public boolean isConstantLocation() {
        return false;
    }

    @Override
    public boolean isValueConstant() {
        return false;
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public <T> long accept(ValueHandleVisitorLong<T> visitor, T param) {
        return visitor.visit(param, this);
    }
}

package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.PhysicalObjectType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 * A field handle for an instance field.
 */
public final class InstanceFieldOf extends Field {
    private final ValueHandle instance;
    private final PhysicalObjectType instanceType;

    InstanceFieldOf(ExecutableElement element, int line, int bci, FieldElement fieldElement, ValueType valueType, ValueHandle instance) {
        super(element, line, bci, fieldElement, valueType);
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

    public PhysicalObjectType getInstanceType() {
        return instanceType;
    }

    int calcHashCode() {
        return super.calcHashCode() * 19 + instance.hashCode();
    }

    public boolean equals(final Object other) {
        return other instanceof InstanceFieldOf && equals((InstanceFieldOf) other);
    }

    public boolean equals(final InstanceFieldOf other) {
        return this == other || other != null && getVariableElement().equals(other.getVariableElement()) && instance.equals(other.instance);
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

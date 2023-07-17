package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.PointerType;
import org.qbicc.type.definition.element.InstanceFieldElement;

/**
 * A pointer to an instance field.
 */
public final class InstanceFieldOf extends AbstractValue {
    private final Value instancePointer;
    private final InstanceFieldElement field;
    private final PointerType type;

    InstanceFieldOf(final ProgramLocatable pl, Value instancePointer, InstanceFieldElement field) {
        super(pl);
        this.instancePointer = instancePointer;
        instancePointer.getType(PointerType.class);
        this.field = field;
        type = field.getType().getPointer();
    }

    public Value getInstance() {
        return instancePointer;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> instancePointer;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public PointerType getType() {
        return type;
    }

    public InstanceFieldElement getVariableElement() {
        return field;
    }

    int calcHashCode() {
        return Objects.hash(instancePointer, field);
    }

    @Override
    String getNodeName() {
        return "InstanceFieldOf";
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        b.append("field pointer ");
        instancePointer.toReferenceString(b);
        b.append('.');
        b.append(field.getName());
        return b;
    }

    public boolean equals(final Object other) {
        return other instanceof InstanceFieldOf ifo && equals(ifo);
    }

    public boolean equals(final InstanceFieldOf other) {
        return this == other || other != null && getVariableElement().equals(other.getVariableElement()) && instancePointer.equals(other.instancePointer);
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

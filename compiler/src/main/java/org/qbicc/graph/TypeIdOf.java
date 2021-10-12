package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.TypeType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * The type ID of a given value.
 */
public final class TypeIdOf extends AbstractValue {
    private final ValueHandle instance;
    private final TypeType type;

    TypeIdOf(final Node callSite, final ExecutableElement element, final int line, final int bci, final ValueHandle instance) {
        super(callSite, element, line, bci);
        this.instance = instance;
        PhysicalObjectType pot = (PhysicalObjectType)instance.getValueType();
        type = pot.getTypeType();
    }

    public TypeType getType() {
        return type;
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    @Override
    public ValueHandle getValueHandle() {
        return instance;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean isConstant() {
        return instance.isValueConstant();
    }

    int calcHashCode() {
        return Objects.hash(TypeIdOf.class, instance);
    }

    @Override
    String getNodeName() {
        return "TypeIdOf";
    }

    public boolean equals(final Object other) {
        return other instanceof TypeIdOf && equals((TypeIdOf) other);
    }

    public boolean equals(final TypeIdOf other) {
        return this == other || other != null
            && instance.equals(other.instance);
    }
}

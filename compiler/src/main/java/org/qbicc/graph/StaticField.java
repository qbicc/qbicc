package org.qbicc.graph;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;

/**
 * A field handle for a static field.
 */
public final class StaticField extends Field {

    StaticField(ExecutableElement element, int line, int bci, FieldElement fieldElement, ValueType valueType) {
        super(element, line, bci, fieldElement, valueType);
    }

    public boolean equals(final Object other) {
        return other instanceof StaticField && equals((StaticField) other);
    }

    public boolean equals(final StaticField other) {
        return this == other || other != null && getVariableElement().equals(other.getVariableElement());
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

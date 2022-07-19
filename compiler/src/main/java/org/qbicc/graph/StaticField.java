package org.qbicc.graph;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.StaticFieldElement;

/**
 * A field handle for a static field.
 */
public final class StaticField extends Field {

    StaticField(ExecutableElement element, int line, int bci, StaticFieldElement fieldElement, ValueType valueType) {
        super(element, line, bci, fieldElement, valueType.getPointer());
    }

    @Override
    String getNodeName() {
        return "StaticField";
    }

    @Override
    public StaticFieldElement getVariableElement() {
        return (StaticFieldElement) super.getVariableElement();
    }

    public boolean equals(final Object other) {
        return other instanceof StaticField sf && equals(sf);
    }

    public boolean equals(final StaticField other) {
        return this == other || other != null && getVariableElement().equals(other.getVariableElement());
    }

    public boolean isConstantLocation() {
        return true;
    }

    @Override
    public boolean isValueConstant() {
        return getVariableElement().getInitialValue() != null || getVariableElement().isReallyFinal();
    }

    @Override
    public <T, R> R accept(PointerValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final PointerValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

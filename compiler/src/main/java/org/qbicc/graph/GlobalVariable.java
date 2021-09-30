package org.qbicc.graph;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.GlobalVariableElement;

/**
 *
 */
public final class GlobalVariable extends Variable {
    GlobalVariable(ExecutableElement element, int line, int bci, GlobalVariableElement variableElement, ValueType valueType) {
        super(element, line, bci, variableElement, valueType.getPointer());
    }

    @Override
    public GlobalVariableElement getVariableElement() {
        return (GlobalVariableElement) super.getVariableElement();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GlobalVariable && equals((GlobalVariable) other);
    }

    public boolean equals(GlobalVariable other) {
        return this == other || other != null && getVariableElement().equals(other.getVariableElement());
    }

    public boolean isConstantLocation() {
        return true;
    }

    @Override
    public boolean isValueConstant() {
        // todo: detect constant global variables
        return false;
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueHandleVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

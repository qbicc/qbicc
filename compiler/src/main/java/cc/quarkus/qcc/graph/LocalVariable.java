package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.LocalVariableElement;

/**
 *
 */
public final class LocalVariable extends Variable {
    LocalVariable(ExecutableElement element, int line, int bci, LocalVariableElement variableElement, ValueType valueType) {
        super(element, line, bci, variableElement, valueType);
    }

    @Override
    public LocalVariableElement getVariableElement() {
        return (LocalVariableElement) super.getVariableElement();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LocalVariable && equals((LocalVariable) other);
    }

    public boolean equals(LocalVariable other) {
        return this == other || other != null && getVariableElement().equals(other.getVariableElement());
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

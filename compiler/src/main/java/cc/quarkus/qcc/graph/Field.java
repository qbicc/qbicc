package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 *
 */
public abstract class Field extends Variable {
    public Field(final ExecutableElement element, final int line, final int bci, final FieldElement variableElement, final ValueType valueType) {
        super(element, line, bci, variableElement, valueType);
    }

    @Override
    public FieldElement getVariableElement() {
        return (FieldElement) super.getVariableElement();
    }
}

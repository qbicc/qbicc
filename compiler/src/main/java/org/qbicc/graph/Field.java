package org.qbicc.graph;

import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
public abstract class Field extends Variable {
    public Field(final ExecutableElement element, final int line, final int bci, final FieldElement variableElement, final PointerType pointerType) {
        super(element, line, bci, variableElement, pointerType);
    }

    @Override
    public FieldElement getVariableElement() {
        return (FieldElement) super.getVariableElement();
    }
}

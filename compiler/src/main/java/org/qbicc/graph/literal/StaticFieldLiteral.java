package org.qbicc.graph.literal;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.definition.element.VariableElement;

/**
 * A literal for a static field.
 */
public class StaticFieldLiteral extends VariableLiteral {
    StaticFieldLiteral(StaticFieldElement variable) {
        super(variable);
    }

    StaticFieldLiteral(VariableElement variable) {
        this((StaticFieldElement) variable);
    }

    @Override
    public StaticFieldElement getVariableElement() {
        return (StaticFieldElement) super.getVariableElement();
    }

    @Override
    public StringBuilder toReferenceString(StringBuilder b) {
        StaticFieldElement sfe = getVariableElement();
        return b.append('@').append(sfe.getEnclosingType().getInternalName().replace('/', '.')).append('.').append(sfe.getName());
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

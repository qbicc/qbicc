package org.qbicc.graph.literal;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.VariableElement;

/**
 * A literal for a global variable.
 */
public class GlobalVariableLiteral extends VariableLiteral {
    GlobalVariableLiteral(GlobalVariableElement variable) {
        super(variable);
    }

    GlobalVariableLiteral(VariableElement variable) {
        this((GlobalVariableElement) variable);
    }

    @Override
    public GlobalVariableElement getVariableElement() {
        return (GlobalVariableElement) super.getVariableElement();
    }

    @Override
    public StringBuilder toReferenceString(StringBuilder b) {
        GlobalVariableElement gve = getVariableElement();
        return b.append('@').append(gve.getName());
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}

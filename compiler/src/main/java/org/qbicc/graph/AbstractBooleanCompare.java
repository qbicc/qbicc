package org.qbicc.graph;

import org.qbicc.type.BooleanType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public abstract class AbstractBooleanCompare extends AbstractBinaryValue implements BooleanValue {
    private final BooleanType booleanType;

    AbstractBooleanCompare(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value left, final Value right, final BooleanType booleanType) {
        super(callSite, element, line, bci, left, right);
        this.booleanType = booleanType;
    }

    @Override
    public BooleanType getType() {
        return booleanType;
    }

    @Override
    public Value getValueIfTrue(Value input) {
        if (equals(input)) {
            return getElement().getEnclosingType().getContext().getLiteralFactory().literalOf(true);
        }
        return input;
    }

    @Override
    public Value getValueIfFalse(Value input) {
        if (equals(input)) {
            return getElement().getEnclosingType().getContext().getLiteralFactory().literalOf(false);
        }
        return input;
    }
}

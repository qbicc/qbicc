package org.qbicc.graph;

import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.type.BooleanType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class IsNe extends AbstractBooleanCompare implements CommutativeBinaryValue {
    IsNe(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value v1, final Value v2, final BooleanType booleanType) {
        super(callSite, element, line, bci, v1, v2, booleanType);
    }

    @Override
    public Value getValueIfTrue(Value input) {
        if (input.getType() instanceof ReferenceType) {
            if (input.equals(getLeftInput()) && getRightInput() instanceof ZeroInitializerLiteral ||
                input.equals(getRightInput()) && getLeftInput() instanceof ZeroInitializerLiteral) {
                // todo: maybe require a BBB input instead
                return new NotNull(input.getCallSite(), input.getElement(), input.getSourceLine(), input.getBytecodeIndex(), input);
            }
        }
        return input;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

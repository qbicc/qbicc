package org.qbicc.graph;

import org.qbicc.type.IntegerType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class CountTrailingZeros extends AbstractUnaryValue {
    private final IntegerType resultType;

    CountTrailingZeros(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value v, final IntegerType resultType) {
        super(callSite, element, line, bci, v);
        this.resultType = resultType;
    }

    @Override
    public IntegerType getType() {
        return resultType;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}

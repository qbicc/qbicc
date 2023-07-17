package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.IntegerType;

/**
 *
 */
public final class CountTrailingZeros extends AbstractUnaryValue {
    private final IntegerType resultType;

    CountTrailingZeros(final ProgramLocatable pl, final Value v, final IntegerType resultType) {
        super(pl, v);
        this.resultType = resultType;
    }

    @Override
    public IntegerType getType() {
        return resultType;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "CountTrailingZeros";
    }
}

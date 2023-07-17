package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.IntegerType;

/**
 *
 */
public final class BitReverse extends AbstractUnaryValue {
    BitReverse(final ProgramLocatable pl, final Value v) {
        super(pl, v);
        //noinspection RedundantClassCall
        IntegerType.class.cast(v.getType());
    }

    @Override
    public IntegerType getType() {
        return (IntegerType) super.getType();
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "BitReverse";
    }
}

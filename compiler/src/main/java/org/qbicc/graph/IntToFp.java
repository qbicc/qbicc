package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;

/**
 * An integer to floating point value conversion.
 */
public final class IntToFp extends AbstractWordCastValue {
    IntToFp(final ProgramLocatable pl, final Value value, final FloatType toType) {
        super(pl, value, toType);
        value.getType(IntegerType.class);
    }

    @Override
    public FloatType getType() {
        return (FloatType) super.getType();
    }

    @Override
    public IntegerType getInputType() {
        return super.getInputType(IntegerType.class);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    boolean equals(AbstractWordCastValue other) {
        return other instanceof IntToFp i2f && equals(i2f);
    }

    boolean equals(IntToFp other) {
        return super.equals(other);
    }

    @Override
    String getNodeName() {
        return "IntToFp";
    }
}

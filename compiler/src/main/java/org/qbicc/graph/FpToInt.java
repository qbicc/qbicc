package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;

/**
 * A floating point to integer value conversion.
 */
public final class FpToInt extends AbstractWordCastValue {
    FpToInt(final ProgramLocatable pl, final Value value, final IntegerType toType) {
        super(pl, value, toType);
        value.getType(FloatType.class);
    }

    @Override
    public IntegerType getType() {
        return (IntegerType) super.getType();
    }

    @Override
    public FloatType getInputType() {
        return super.getInputType(FloatType.class);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    boolean equals(AbstractWordCastValue other) {
        return other instanceof FpToInt f2i && equals(f2i);
    }

    boolean equals(FpToInt other) {
        return super.equals(other);
    }

    @Override
    String getNodeName() {
        return "FpToInt";
    }
}

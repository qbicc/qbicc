package org.qbicc.graph;

import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A floating point to integer value conversion.
 */
public final class FpToInt extends AbstractWordCastValue {
    FpToInt(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value value, final IntegerType toType) {
        super(callSite, element, line, bci, value, toType);
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
    String getNodeName() {
        return "FpToInt";
    }
}

package org.qbicc.graph;

import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * An integer to floating point value conversion.
 */
public final class IntToFp extends AbstractWordCastValue {
    IntToFp(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value value, final FloatType toType) {
        super(callSite, element, line, bci, value, toType);
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
    String getNodeName() {
        return "IntToFp";
    }
}

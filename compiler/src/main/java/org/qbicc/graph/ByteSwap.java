package org.qbicc.graph;

import org.qbicc.type.IntegerType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class ByteSwap extends AbstractUnaryValue {
    ByteSwap(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value v) {
        super(callSite, element, line, bci, v);
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
        return "ByteSwap";
    }
}

package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

/**
 *
 */
final class ArrayOf extends AbstractValue {
    private final int dimension;
    private final AbstractValue elementType;

    ArrayOf(final int dimension, final AbstractValue elementType) {
        this.dimension = dimension;
        this.elementType = elementType;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return elementType.appendTo(target.append('[').append(Integer.toString(dimension)).append(" x ")).append(']');
    }
}

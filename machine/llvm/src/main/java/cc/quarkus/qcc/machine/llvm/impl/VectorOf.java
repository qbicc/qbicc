package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

/**
 *
 */
final class VectorOf extends AbstractValue {
    private final int dimension;
    private final AbstractValue elementType;
    private final boolean vscale;

    VectorOf(final int dimension, final AbstractValue elementType, final boolean vscale) {
        this.dimension = dimension;
        this.elementType = elementType;
        this.vscale = vscale;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('<');
        if (vscale) {
            target.append("vscale x ");
        }
        return elementType.appendTo(target.append(Integer.toString(dimension)).append(" x ")).append('>');
    }
}

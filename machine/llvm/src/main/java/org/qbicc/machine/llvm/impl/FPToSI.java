package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class FPToSI extends AbstractCastInstruction {
    FPToSI(final BasicBlockImpl block, final AbstractValue type, final AbstractValue value, final AbstractValue toType) {
        super(block, type, value, toType);
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendTrailer(super.appendTo(target).append("fptosi"));
    }
}

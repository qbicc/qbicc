package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class FNegImpl extends AbstractFastMathUnary {
    FNegImpl(final BasicBlockImpl block, final AbstractValue type, final AbstractValue arg) {
        super(block, type, arg);
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendTrailer(super.appendTo(target).append("fneg"));
    }
}

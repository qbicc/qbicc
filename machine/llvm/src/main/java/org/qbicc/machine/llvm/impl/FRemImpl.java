package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class FRemImpl extends AbstractFastMathBinary {
    FRemImpl(final BasicBlockImpl block, final AbstractValue type, final AbstractValue arg1, final AbstractValue arg2) {
        super(block, type, arg1, arg2);
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendTrailer(super.appendTo(target).append("frem"));
    }
}

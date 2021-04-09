package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.FloatCondition;

final class FCmpImpl extends AbstractFastMathBinary {
    private final FloatCondition cond;

    FCmpImpl(final BasicBlockImpl block, final FloatCondition cond, final AbstractValue type, final AbstractValue arg1, final AbstractValue arg2) {
        super(block, type, arg1, arg2);
        this.cond = cond;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendTrailer(appendMathFlags(super.appendTo(target).append("fcmp")).append(' ').append(cond.name()));
    }
}

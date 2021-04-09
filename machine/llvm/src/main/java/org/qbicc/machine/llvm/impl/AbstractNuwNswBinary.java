package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.NuwNswBinary;

abstract class AbstractNuwNswBinary extends AbstractBinary implements NuwNswBinary {
    boolean nuw, nsw;

    AbstractNuwNswBinary(final BasicBlockImpl block, final AbstractValue type, final AbstractValue arg1, final AbstractValue arg2) {
        super(block, type, arg1, arg2);
    }

    public NuwNswBinary meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public NuwNswBinary comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public NuwNswBinary nuw() {
        nuw = true;
        return this;
    }

    public NuwNswBinary nsw() {
        nsw = true;
        return this;
    }

    Appendable appendTrailer(final Appendable target) throws IOException {
        if (nuw) {
            target.append(' ').append("nuw");
        }
        if (nsw) {
            target.append(' ').append("nsw");
        }
        return super.appendTrailer(target);
    }
}

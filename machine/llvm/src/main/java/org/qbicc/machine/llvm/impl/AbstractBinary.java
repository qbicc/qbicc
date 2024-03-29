package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.Binary;

abstract class AbstractBinary extends AbstractYieldingInstruction implements Binary {
    final AbstractValue type;
    final AbstractValue arg1;
    final AbstractValue arg2;

    AbstractBinary(final BasicBlockImpl block, final AbstractValue type, final AbstractValue arg1, final AbstractValue arg2) {
        super(block);
        this.type = type;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public Binary meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Binary comment(final String comment) {
        super.comment(comment);
        return this;
    }

    Appendable appendTrailer(final Appendable target) throws IOException {
        target.append(' ');
        type.appendTo(target);
        target.append(' ');
        arg1.appendTo(target);
        target.append(',').append(' ');
        arg2.appendTo(target);
        return super.appendTrailer(target);
    }
}

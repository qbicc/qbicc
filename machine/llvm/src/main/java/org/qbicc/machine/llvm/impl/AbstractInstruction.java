package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.Instruction;

abstract class AbstractInstruction extends AbstractMetable implements Instruction {

    AbstractInstruction() {
        metaHasComma = true;
    }

    public Instruction meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Instruction comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return target;
    }
}

package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.Return;

abstract class AbstractReturn extends AbstractInstruction implements Return {
    AbstractReturn() {
        super();
    }

    public Return meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Return comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return super.appendTo(target).append("ret");
    }
}

package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.op.Instruction;

abstract class AbstractInstruction extends AbstractMetable implements Instruction {

    AbstractInstruction() {
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

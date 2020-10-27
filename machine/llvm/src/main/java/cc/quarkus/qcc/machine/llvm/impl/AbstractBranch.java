package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.op.Branch;

abstract class AbstractBranch extends AbstractInstruction implements Branch {
    AbstractBranch() {
        super();
    }

    public Branch meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Branch comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return super.appendTo(target).append("br");
    }
}

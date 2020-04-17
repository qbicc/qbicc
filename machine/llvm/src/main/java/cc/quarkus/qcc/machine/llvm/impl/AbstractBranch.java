package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.op.Branch;

abstract class AbstractBranch extends AbstractInstruction implements Branch {
    AbstractBranch() {
        super();
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return super.appendTo(target).append("br");
    }
}

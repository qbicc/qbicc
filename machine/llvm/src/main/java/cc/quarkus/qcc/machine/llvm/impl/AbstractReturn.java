package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.op.Return;

abstract class AbstractReturn extends AbstractInstruction implements Return {
    AbstractReturn() {
        super();
    }

    public Return meta(final String name, final Value data) {
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

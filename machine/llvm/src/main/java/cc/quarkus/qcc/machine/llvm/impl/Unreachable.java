package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

final class Unreachable extends AbstractInstruction {
    static final Unreachable INSTANCE = new Unreachable();

    private Unreachable() {
        super();
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendTrailer(super.appendTo(target).append("unreachable"));
    }
}

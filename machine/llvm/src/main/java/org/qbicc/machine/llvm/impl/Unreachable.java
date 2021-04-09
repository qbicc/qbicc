package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class Unreachable extends AbstractInstruction {
    Unreachable() {
        super();
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendTrailer(super.appendTo(target).append("unreachable"));
    }
}

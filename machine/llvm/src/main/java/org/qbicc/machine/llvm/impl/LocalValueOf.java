package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class LocalValueOf extends AbstractValue {
    final int index;

    LocalValueOf(final AbstractInstruction instruction, final int index) {
        super(instruction);
        this.index = index;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append("%tmp");
        return appendDecimal(target, index);
    }
}

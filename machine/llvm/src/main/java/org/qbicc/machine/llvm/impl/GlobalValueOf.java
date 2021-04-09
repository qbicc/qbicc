package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class GlobalValueOf extends AbstractValue {
    final AbstractInstruction instruction;
    final int index;

    GlobalValueOf(final AbstractInstruction instruction, final int index) {
        this.instruction = instruction;
        this.index = index;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('@').append('G');
        appendHex(target, index);
        return target;
    }
}

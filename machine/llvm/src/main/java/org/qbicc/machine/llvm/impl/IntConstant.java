package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class IntConstant extends AbstractValue {
    final int value;

    IntConstant(final int value) {
        this.value = value;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendDecimal(target, value);
    }
}

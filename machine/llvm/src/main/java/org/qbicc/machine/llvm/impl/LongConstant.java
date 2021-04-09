package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class LongConstant extends AbstractValue {
    final long value;

    LongConstant(final long value) {
        this.value = value;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendDecimal(target, value);
    }
}

package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.LazyLLValue;
import org.qbicc.machine.llvm.op.YieldingInstruction;

final class LazyValueImpl extends AbstractValue implements LazyLLValue {
    private AbstractValue actual;

    @Override
    public YieldingInstruction getInstruction() {
        if (actual == null) {
            return null;
        }
        return actual.getInstruction();
    }

    @Override
    public Appendable appendTo(Appendable target) throws IOException {
        return actual.appendTo(target);
    }

    @Override
    public void resolveTo(LLValue value) throws IllegalStateException {
        if (actual != null) {
            throw new IllegalStateException();
        }
        actual = (AbstractValue) value;
    }
}

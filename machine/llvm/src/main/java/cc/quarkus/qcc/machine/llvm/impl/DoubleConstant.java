package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

final class DoubleConstant extends AbstractValue {
    final double value;

    DoubleConstant(final double value) {
        this.value = value;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendHex(target, value);
    }
}

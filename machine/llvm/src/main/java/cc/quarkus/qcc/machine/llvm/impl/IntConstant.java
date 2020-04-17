package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

final class IntConstant extends AbstractValue {
    final int value;

    IntConstant(final int value) {
        this.value = value;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendHex(target.append("0x"), value);
    }
}

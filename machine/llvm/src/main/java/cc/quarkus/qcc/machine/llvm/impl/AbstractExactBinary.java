package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.op.ExactBinary;

abstract class AbstractExactBinary extends AbstractBinary implements ExactBinary {
    boolean exact;

    AbstractExactBinary(final BasicBlockImpl block, final AbstractValue type, final AbstractValue arg1, final AbstractValue arg2) {
        super(block, type, arg1, arg2);
    }

    public ExactBinary meta(final String name, final Value data) {
        super.meta(name, data);
        return this;
    }

    public ExactBinary exact() {
        exact = true;
        return this;
    }

    Appendable appendTrailer(final Appendable target) throws IOException {
        if (exact) {
            target.append(' ').append("exact");
        }
        return super.appendTrailer(target);
    }
}

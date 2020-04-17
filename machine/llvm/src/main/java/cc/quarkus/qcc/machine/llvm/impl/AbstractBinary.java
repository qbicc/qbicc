package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.op.Binary;

abstract class AbstractBinary extends AbstractYieldingInstruction implements Binary {
    final AbstractValue type;
    final AbstractValue arg1;
    final AbstractValue arg2;

    AbstractBinary(final BasicBlockImpl block, final AbstractValue type, final AbstractValue arg1, final AbstractValue arg2) {
        super(block);
        this.type = type;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public Binary meta(final String name, final Value data) {
        super.meta(name, data);
        return this;
    }

    Appendable appendTrailer(final Appendable target) throws IOException {
        type.appendTo(target);
        target.append(' ');
        arg1.appendTo(target);
        target.append(',').append(' ');
        arg2.appendTo(target);
        return super.appendTrailer(target);
    }
}

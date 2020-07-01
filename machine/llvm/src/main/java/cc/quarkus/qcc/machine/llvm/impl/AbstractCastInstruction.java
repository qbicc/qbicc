package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

abstract class AbstractCastInstruction extends AbstractYieldingInstruction {
    final AbstractValue type;
    final AbstractValue value;
    final AbstractValue toType;

    AbstractCastInstruction(final BasicBlockImpl block, final AbstractValue type, final AbstractValue value, final AbstractValue toType) {
        super(block);
        this.type = type;
        this.value = value;
        this.toType = toType;
    }

    Appendable appendTrailer(final Appendable target) throws IOException {
        target.append(' ');
        type.appendTo(target);
        target.append(' ');
        value.appendTo(target);
        target.append(' ');
        target.append("to");
        target.append(' ');
        toType.appendTo(target);
        return super.appendTrailer(target);
    }
}

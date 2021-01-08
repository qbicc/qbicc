package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

final class IntToPtr extends AbstractCastInstruction {
    IntToPtr(final BasicBlockImpl block, final AbstractValue type, final AbstractValue value, final AbstractValue toType) {
        super(block, type, value, toType);
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendTrailer(super.appendTo(target).append("inttoptr"));
    }
}

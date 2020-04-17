package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.op.Return;

final class ValueReturn extends AbstractReturn implements Return {

    private final AbstractValue type;
    private final AbstractValue val;

    ValueReturn(final AbstractValue type, final AbstractValue val) {
        super();
        this.type = type;
        this.val = val;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendTrailer(val.appendTo(type.appendTo(super.appendTo(target).append(' ')).append(' ')));
    }
}

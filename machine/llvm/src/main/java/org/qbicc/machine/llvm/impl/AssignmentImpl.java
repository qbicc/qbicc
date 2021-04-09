package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.Assignment;

/**
 *
 */
final class AssignmentImpl extends AbstractYieldingInstruction implements Assignment {
    private final AbstractValue rvalue;

    AssignmentImpl(final BasicBlockImpl block, final AbstractValue value) {
        super(block);
        rvalue = value;
    }

    public Assignment meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Assignment comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendTrailer(rvalue.appendTo(super.appendTo(target)));
    }
}

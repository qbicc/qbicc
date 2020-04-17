package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.op.Assignment;

/**
 *
 */
final class AssignmentImpl extends AbstractYieldingInstruction implements Assignment {
    private final AbstractValue rvalue;

    AssignmentImpl(final BasicBlockImpl block, final AbstractValue value) {
        super(block);
        rvalue = value;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendTrailer(rvalue.appendTo(super.appendTo(target)));
    }
}

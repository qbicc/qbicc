package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.Types;

final class ConditionalBranchImpl extends AbstractBranch {
    private final AbstractValue cond;
    private final BasicBlockImpl ifTrue;
    private final BasicBlockImpl ifFalse;

    ConditionalBranchImpl(final AbstractValue cond, final BasicBlockImpl ifTrue, final BasicBlockImpl ifFalse) {
        super();
        this.cond = cond;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append(' ');
        appendValue(target, Types.i1);
        target.append(' ');
        cond.appendTo(target);
        target.append(',').append(' ');
        target.append("label");
        target.append(' ');
        ifTrue.appendTo(target);
        target.append(',').append(' ');
        target.append("label");
        target.append(' ');
        ifFalse.appendTo(target);
        return appendTrailer(target);
    }
}

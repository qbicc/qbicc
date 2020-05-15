package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

final class UnconditionalBranchImpl extends AbstractBranch {
    private final BasicBlockImpl dest;

    UnconditionalBranchImpl(final BasicBlockImpl dest) {
        super();
        this.dest = dest;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendTrailer(dest.appendTo(super.appendTo(target).append(' ').append("label").append(' ')));
    }
}

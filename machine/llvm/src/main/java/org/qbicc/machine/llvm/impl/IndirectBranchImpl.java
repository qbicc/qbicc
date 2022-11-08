package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.llvm.LLBasicBlock;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.IndirectBranch;

final class IndirectBranchImpl extends AbstractBranch implements IndirectBranch {
    final AbstractValue address;
    Target last;

    IndirectBranchImpl(final AbstractValue address) {
        this.address = address;
    }

    @Override
    public IndirectBranch meta(String name, LLValue data) {
        super.meta(name, data);
        return this;
    }

    @Override
    public IndirectBranch comment(String comment) {
        super.comment(comment);
        return this;
    }

    @Override
    public IndirectBranch possibleTarget(LLBasicBlock target) {
        last = new Target(Assert.checkNotNullParam("target", (BasicBlockImpl) target), last);
        return this;
    }

    @Override
    public Appendable appendTo(Appendable target) throws IOException {
        target.append("indirect");
        super.appendTo(target);
        target.append(' ');
        target.append("ptr");
        target.append(' ');
        address.appendTo(target);
        target.append(',');
        target.append(' ');
        target.append('[');
        Target last = this.last;
        if (last != null) {
            target.append(' ');
            last.appendTo(target);
            target.append(' ');
        }
        target.append(']');
        return target;
    }

    static class Target implements Emittable {
        final BasicBlockImpl block;
        final Target prev;

        Target(BasicBlockImpl block, Target prev) {
            this.block = block;
            this.prev = prev;
        }

        @Override
        public Appendable appendTo(Appendable target) throws IOException {
            if (prev != null) {
                prev.appendTo(target);
                target.append(',');
                target.append(' ');
            }
            target.append("label");
            target.append(' ');
            block.appendTo(target);
            return target;
        }
    }
}

package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.Set;

import org.qbicc.machine.llvm.FastMathFlag;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.FastMathUnary;
import io.smallrye.common.constraint.Assert;

abstract class AbstractFastMathUnary extends org.qbicc.machine.llvm.impl.AbstractYieldingInstruction implements FastMathUnary {
    final AbstractValue type;
    final AbstractValue arg;
    Set<FastMathFlag> flags = Set.of();

    AbstractFastMathUnary(final BasicBlockImpl block, final AbstractValue type, final AbstractValue arg) {
        super(block);
        this.type = type;
        this.arg = arg;
    }

    public FastMathUnary withFlags(final Set<FastMathFlag> flags) {
        Assert.checkNotNullParam("flags", flags);
        this.flags = flags;
        return this;
    }

    public FastMathUnary meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public FastMathUnary comment(final String comment) {
        super.comment(comment);
        return this;
    }

    Appendable appendTrailer(final Appendable target) throws IOException {
        for (FastMathFlag flag : flags) {
            target.append(' ').append(flag.name());
        }
        target.append(' ');
        type.appendTo(target);
        target.append(' ');
        arg.appendTo(target);
        return super.appendTrailer(target);
    }
}

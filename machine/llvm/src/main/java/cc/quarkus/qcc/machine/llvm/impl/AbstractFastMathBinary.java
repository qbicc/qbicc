package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.Set;

import org.qbicc.machine.llvm.FastMathFlag;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.FastMathBinary;
import io.smallrye.common.constraint.Assert;

abstract class AbstractFastMathBinary extends AbstractBinary implements FastMathBinary {
    Set<FastMathFlag> flags = Set.of();

    AbstractFastMathBinary(final BasicBlockImpl block, final AbstractValue type, final AbstractValue arg1, final AbstractValue arg2) {
        super(block, type, arg1, arg2);
    }

    public FastMathBinary withFlags(final Set<FastMathFlag> flags) {
        Assert.checkNotNullParam("flags", flags);
        this.flags = flags;
        return this;
    }

    public FastMathBinary meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public FastMathBinary comment(final String comment) {
        super.comment(comment);
        return this;
    }

    protected Appendable appendMathFlags(Appendable target) throws IOException {
        for (FastMathFlag flag : flags) {
            target.append(' ').append(flag.name());
        }
        return target;
    }
}

package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.Set;

import org.qbicc.machine.llvm.FastMathFlag;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.Select;
import io.smallrye.common.constraint.Assert;

final class SelectImpl extends AbstractYieldingInstruction implements Select {
    private final AbstractValue condType;
    private final AbstractValue cond;
    private final AbstractValue valueType;
    private final AbstractValue trueValue;
    private final AbstractValue falseValue;
    Set<FastMathFlag> flags = Set.of();

    public SelectImpl(final BasicBlockImpl block, final AbstractValue condType, final AbstractValue cond, final AbstractValue valueType, final AbstractValue trueValue, final AbstractValue falseValue) {
        super(block);
        this.condType = condType;
        this.cond = cond;
        this.valueType = valueType;
        this.trueValue = trueValue;
        this.falseValue = falseValue;
    }

    public Select meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Select comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public Select withFlags(final Set<FastMathFlag> flagSet) {
        Assert.checkNotNullParam("flagSet", flagSet);
        this.flags = flagSet;
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append("select");
        for (FastMathFlag flag : flags) {
            target.append(' ').append(flag.name());
        }
        target.append(' ');
        condType.appendTo(target);
        target.append(' ');
        cond.appendTo(target);
        target.append(',').append(' ');
        valueType.appendTo(target);
        target.append(' ');
        trueValue.appendTo(target);
        target.append(',').append(' ');
        valueType.appendTo(target);
        target.append(' ');
        falseValue.appendTo(target);
        return appendTrailer(target);
    }
}

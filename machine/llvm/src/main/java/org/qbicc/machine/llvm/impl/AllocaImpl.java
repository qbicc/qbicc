package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.Alloca;
import io.smallrye.common.constraint.Assert;

final class AllocaImpl extends AbstractYieldingInstruction implements Alloca {
    private final AbstractValue type;
    private AbstractValue numElementsType;
    private AbstractValue numElements;
    private AbstractValue align;

    AllocaImpl(final BasicBlockImpl block, final AbstractValue type) {
        super(block);
        this.type = type;
    }

    public Alloca elements(final LLValue type, final LLValue count) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("count", count);
        numElementsType = (AbstractValue) type;
        numElements = (AbstractValue) count;
        return this;
    }

    public Alloca align(final LLValue align) {
        Assert.checkNotNullParam("align", align);
        this.align = (AbstractValue) align;
        return this;
    }

    public Alloca comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public Alloca meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append("alloca");
        target.append(' ');
        type.appendTo(target);
        if (numElements != null) {
            target.append(',');
            target.append(' ');
            numElementsType.appendTo(target);
            target.append(' ');
            numElements.appendTo(target);
        }
        if (align != null) {
            target.append(',');
            target.append(' ');
            target.append("align");
            target.append(' ');
            align.appendTo(target);
        }
        return target;
    }
}

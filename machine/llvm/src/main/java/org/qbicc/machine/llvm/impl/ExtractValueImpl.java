package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.ExtractValue;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class ExtractValueImpl extends AbstractYieldingInstruction implements ExtractValue {
    private final AbstractValue aggregateType;
    private final AbstractValue aggregate;
    ArgImpl lastArg;

    ExtractValueImpl(final BasicBlockImpl basicBlock, final AbstractValue aggregateType, final AbstractValue aggregate) {
        super(basicBlock);
        this.aggregateType = aggregateType;
        this.aggregate = aggregate;
    }

    public ExtractValue comment(final String comment) {
        return (ExtractValue) super.comment(comment);
    }

    public ExtractValue meta(final String name, final LLValue data) {
        return (ExtractValue) super.meta(name, data);
    }

    public ExtractValue arg(final LLValue index) {
        Assert.checkNotNullParam("index", index);
        lastArg = new ArgImpl((AbstractValue) index, lastArg);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append("extractvalue");
        target.append(' ');
        aggregateType.appendTo(target);
        target.append(' ');
        aggregate.appendTo(target);
        lastArg.appendTo(target);
        return appendTrailer(target);
    }

    static final class ArgImpl extends AbstractEmittable {
        final AbstractValue index;
        final ArgImpl prev;

        ArgImpl(final AbstractValue index, final ArgImpl prev) {
            this.index = index;
            this.prev = prev;
        }

        public Appendable appendTo(final Appendable target) throws IOException {
            ArgImpl prev = this.prev;
            if (prev != null) {
                prev.appendTo(target);
            }
            target.append(',');
            target.append(' ');
            index.appendTo(target);
            return target;
        }
    }
}

package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.ExtractValue;

/**
 *
 */
final class InsertValueImpl extends AbstractYieldingInstruction implements ExtractValue {
    private final AbstractValue aggregateType;
    private final AbstractValue aggregate;
    private final AbstractValue insertType;
    private final AbstractValue insert;
    ArgImpl lastArg;

    InsertValueImpl(final BasicBlockImpl basicBlock, final AbstractValue aggregateType, final AbstractValue aggregate, AbstractValue insertType, AbstractValue insert) {
        super(basicBlock);
        this.aggregateType = aggregateType;
        this.aggregate = aggregate;
        this.insertType = insertType;
        this.insert = insert;
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
        target.append("insertvalue");
        target.append(' ');
        aggregateType.appendTo(target);
        target.append(' ');
        aggregate.appendTo(target);
        target.append(',');
        target.append(' ');
        insertType.appendTo(target);
        target.append(' ');
        insert.appendTo(target);
        ArgImpl lastArg = this.lastArg;
        if (lastArg != null) {
            lastArg.appendTo(target);
        }
        return target;
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

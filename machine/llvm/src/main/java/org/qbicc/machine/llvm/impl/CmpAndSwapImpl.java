package org.qbicc.machine.llvm.impl;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.CmpAndSwap;
import org.qbicc.machine.llvm.op.OrderingConstraint;

import java.io.IOException;

final class CmpAndSwapImpl extends AbstractYieldingInstruction implements CmpAndSwap {
    private final AbstractValue pointerType;
    private final AbstractValue type;
    private final AbstractValue pointer;
    private final AbstractValue expect;
    private final AbstractValue update;
    private final OrderingConstraint successOrdering;
    private final OrderingConstraint failureOrdering;
    /* optional arguments */
    private boolean weak;   /* not mapped from add phase */
    private boolean isVolatile; /* not mapped from add phase */
    private String syncScope;   /* not mapped from add phase */
    private int alignment;  /* not mapped from add phase */

    public CmpAndSwapImpl(BasicBlockImpl block, AbstractValue pointerType, AbstractValue type, AbstractValue pointer,
                          AbstractValue expect, AbstractValue update, OrderingConstraint successOrdering,
                          OrderingConstraint failureOrdering) {
        super(block);
        this.pointerType = pointerType;
        this.type = type;
        this.pointer = pointer;
        this.expect = expect;
        this.update = update;
        this.successOrdering = successOrdering;
        this.failureOrdering = failureOrdering;
        this.isVolatile = true; /* default for Java */
    }

    public CmpAndSwap comment(final String comment) {
        return (CmpAndSwap)super.comment(comment);
    }

    public CmpAndSwap meta(final String name, final LLValue data) {
        return (CmpAndSwap)super.meta(name, data);
    }

    public CmpAndSwap weak() {
        weak = true;
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append("cmpxchg");

        /* weak */
        if (weak) {
            space(target);
            target.append("weak");
        }

        /* volatile */
        if (isVolatile) {
            space(target);
            target.append("volatile");
        }

        /* pointer */
        space(target);
        pointerType.appendTo(target);
        space(target);
        pointer.appendTo(target);
        target.append(',');

        /* cmp */
        space(target);
        type.appendTo(target);
        space(target);
        expect.appendTo(target);
        target.append(',');

        /* new */
        space(target);
        type.appendTo(target);
        space(target);
        update.appendTo(target);

        /* syncscope */
        final String syncScope = this.syncScope;
        if (syncScope != null) {
            space(target);
            target.append("syncscope");
            target.append('(');
            target.append('"');
            target.append(syncScope);
            target.append('"');
            target.append(')');
        }

        /* success ordering */
        space(target);
        target.append(successOrdering.name());

        /* failure ordering */
        space(target);
        target.append(failureOrdering.name());

        /* align */
        if (alignment != 0) {
            target.append(',');
            space(target);
            target.append("align");
            space(target);
            target.append(Integer.toString(alignment));
        }

        return appendTrailer(target);
    }

    private void space(Appendable target) throws IOException {
        target.append(' ');
    }
}

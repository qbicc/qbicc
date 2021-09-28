package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.AtomicRmw;
import org.qbicc.machine.llvm.op.OrderingConstraint;

/**
 *
 */
final class AtomicRmwImpl extends AbstractYieldingInstruction implements AtomicRmw {
    final AbstractValue type;
    final AbstractValue value;
    final AbstractValue pointeeType;
    final AbstractValue pointer;
    int alignment;
    boolean volatile_;
    OrderingConstraint constraint;
    String syncScope;
    Operation operation;

    AtomicRmwImpl(final BasicBlockImpl block, final AbstractValue type, final AbstractValue value, final AbstractValue pointeeType, final AbstractValue pointer) {
        super(block);
        this.type = type;
        this.value = value;
        this.pointeeType = pointeeType;
        this.pointer = pointer;
    }

    @Override
    public AtomicRmw comment(String comment) {
        super.comment(comment);
        return this;
    }

    @Override
    public AtomicRmw meta(String name, LLValue data) {
        super.meta(name, data);
        return this;
    }

    @Override
    public AtomicRmw align(int alignment) {
        Assert.checkMinimumParameter("alignment", 1, alignment);
        if (Integer.bitCount(alignment) != 1) {
            throw new IllegalArgumentException("Alignment must be a power of two");
        }
        this.alignment = alignment;
        return this;
    }

    @Override
    public AtomicRmw volatile_() {
        volatile_ = true;
        return this;
    }

    @Override
    public AtomicRmw syncScope(String scopeName) {
        syncScope = Assert.checkNotNullParam("scopeName", scopeName);
        return this;
    }

    @Override
    public AtomicRmw xchg() {
        operation = Operation.xchg;
        return this;
    }

    @Override
    public AtomicRmw add() {
        operation = Operation.add;
        return this;
    }

    @Override
    public AtomicRmw sub() {
        operation = Operation.sub;
        return this;
    }

    @Override
    public AtomicRmw and() {
        operation = Operation.and;
        return this;
    }

    @Override
    public AtomicRmw nand() {
        operation = Operation.nand;
        return this;
    }

    @Override
    public AtomicRmw or() {
        operation = Operation.or;
        return this;
    }

    @Override
    public AtomicRmw xor() {
        operation = Operation.xor;
        return this;
    }

    @Override
    public AtomicRmw max() {
        operation = Operation.max;
        return this;
    }

    @Override
    public AtomicRmw min() {
        operation = Operation.min;
        return this;
    }

    @Override
    public AtomicRmw umax() {
        operation = Operation.umax;
        return this;
    }

    @Override
    public AtomicRmw umin() {
        operation = Operation.umin;
        return this;
    }

    @Override
    public AtomicRmw fadd() {
        operation = Operation.fadd;
        return this;
    }

    @Override
    public AtomicRmw fsub() {
        operation = Operation.fsub;
        return this;
    }

    @Override
    public AtomicRmw ordering(OrderingConstraint order) {
        constraint = Assert.checkNotNullParam("order", order);
        return this;
    }

    @SuppressWarnings("SpellCheckingInspection")
    enum Operation {
        xchg,
        add,
        sub,
        and,
        nand,
        or,
        xor,
        max,
        min,
        umax,
        umin,
        fadd,
        fsub,
        ;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append("atomicrmw");
        if (volatile_) {
            target.append(' ').append("volatile");
        }
        target.append(' ');
        target.append(operation.name());
        target.append(' ');
        type.appendTo(target);
        target.append(' ');
        pointer.appendTo(target);
        target.append(',').append(' ');
        pointeeType.appendTo(target);
        target.append(' ');
        value.appendTo(target);
        final OrderingConstraint constraint = this.constraint;
        final String syncScope = this.syncScope;
        if (syncScope != null) {
            target.append(' ').append("syncScope").append('(').append('"').append(syncScope).append('"').append(')');
        }
        target.append(' ').append(constraint.name());
/* TODO: LLVM bug prevents this from parsing
        if (alignment != 0) {
            target.append(',').append(' ');
            target.append("align").append(' ');
            target.append(Integer.toString(alignment));
        }
*/
        return appendTrailer(target);
    }
}

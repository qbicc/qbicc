package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.op.Load;
import cc.quarkus.qcc.machine.llvm.op.OrderingConstraint;
import io.smallrye.common.constraint.Assert;

final class LoadImpl extends AbstractYieldingInstruction implements Load {

    final AbstractValue type;
    final AbstractValue pointeeType;
    final AbstractValue pointer;
    int alignment;
    boolean volatile_;
    OrderingConstraint constraint;
    String syncScope;

    LoadImpl(final BasicBlockImpl block, final AbstractValue type, final AbstractValue pointeeType, final AbstractValue pointer) {
        super(block);
        this.type = type;
        this.pointeeType = pointeeType;
        this.pointer = pointer;
    }

    public Load meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Load comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public Load align(final int alignment) {
        Assert.checkMinimumParameter("alignment", 1, alignment);
        if (Integer.bitCount(alignment) != 1) {
            throw new IllegalArgumentException("Alignment must be a power of two");
        }
        this.alignment = alignment;
        return this;
    }

    public Load atomic(final OrderingConstraint constraint) {
        Assert.checkNotNullParam("constraint", constraint);
        this.constraint = constraint;
        return this;
    }

    public Load atomic(final OrderingConstraint constraint, final String syncScope) {
        Assert.checkNotNullParam("constraint", constraint);
        Assert.checkNotNullParam("syncScope", syncScope);
        this.constraint = constraint;
        this.syncScope = syncScope;
        return this;
    }

    public Load volatile_() {
        volatile_ = true;
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append("load");
        final OrderingConstraint constraint = this.constraint;
        if (constraint != null) {
            target.append(' ').append("atomic");
        }
        if (volatile_) {
            target.append(' ').append("volatile");
        }
        target.append(' ');
        pointeeType.appendTo(target);
        target.append(',').append(' ');
        type.appendTo(target);
        target.append(' ');
        pointer.appendTo(target);
        if (constraint != null) {
            final String syncScope = this.syncScope;
            if (syncScope != null) {
                target.append(' ').append("syncScope").append('(').append('"').append(syncScope).append('"').append(')');
            }
            target.append(' ').append(constraint.name());
        }
        if (alignment != 0) {
            target.append(',').append(' ');
            target.append("align").append(' ');
            target.append(Integer.toString(alignment));
        }
        return appendTrailer(target);
    }
}

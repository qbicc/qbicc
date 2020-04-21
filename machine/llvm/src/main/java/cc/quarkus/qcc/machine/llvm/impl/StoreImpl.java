package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.op.OrderingConstraint;
import cc.quarkus.qcc.machine.llvm.op.Store;
import io.smallrye.common.constraint.Assert;

final class StoreImpl extends AbstractInstruction implements Store {

    final AbstractValue type;
    final AbstractValue value;
    final AbstractValue pointeeType;
    final AbstractValue pointer;
    int alignment;
    boolean volatile_;
    OrderingConstraint constraint;
    String syncScope;

    StoreImpl(final AbstractValue type, final AbstractValue value, final AbstractValue pointeeType, final AbstractValue pointer) {
        super();
        this.type = type;
        this.value = value;
        this.pointeeType = pointeeType;
        this.pointer = pointer;
    }

    public Store meta(final String name, final Value data) {
        super.meta(name, data);
        return this;
    }

    public Store comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public Store align(final int alignment) {
        Assert.checkMinimumParameter("alignment", 1, alignment);
        if (Integer.bitCount(alignment) != 1) {
            throw new IllegalArgumentException("Alignment must be a power of two");
        }
        this.alignment = alignment;
        return this;
    }

    public Store atomic(final OrderingConstraint constraint) {
        Assert.checkNotNullParam("constraint", constraint);
        this.constraint = constraint;
        return this;
    }

    public Store atomic(final OrderingConstraint constraint, final String syncScope) {
        Assert.checkNotNullParam("constraint", constraint);
        Assert.checkNotNullParam("syncScope", syncScope);
        this.constraint = constraint;
        this.syncScope = syncScope;
        return this;
    }

    public Store volatile_() {
        volatile_ = true;
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append("store");
        final OrderingConstraint constraint = this.constraint;
        if (constraint != null) {
            target.append(' ').append("atomic");
        }
        if (volatile_) {
            target.append(' ').append("volatile");
        }
        target.append(' ');
        type.appendTo(target);
        target.append(' ');
        value.appendTo(target);
        target.append(',').append(' ');
        pointeeType.appendTo(target);
        target.append('*').append(' ');
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

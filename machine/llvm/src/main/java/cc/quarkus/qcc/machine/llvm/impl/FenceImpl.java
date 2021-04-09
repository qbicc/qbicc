package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.Fence;
import org.qbicc.machine.llvm.op.OrderingConstraint;
import io.smallrye.common.constraint.Assert;

final class FenceImpl extends AbstractInstruction implements Fence {
    final OrderingConstraint ordering;
    String syncScope;

    FenceImpl(final OrderingConstraint ordering) {
        this.ordering = ordering;
    }

    public Fence syncScope(final String scopeName) {
        Assert.checkNotNullParam("scopeName", scopeName);
        syncScope = scopeName;
        return this;
    }

    public Fence meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Fence comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append("fence").append(' ');
        final String syncScope = this.syncScope;
        if (syncScope != null) {
            target.append("syncscope").append('(').append('"').append(syncScope).append('"').append(')').append(' ');
        }
        target.append(ordering.name());
        return appendTrailer(target);
    }
}

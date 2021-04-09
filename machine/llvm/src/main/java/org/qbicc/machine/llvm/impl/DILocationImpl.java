package org.qbicc.machine.llvm.impl;

import org.qbicc.machine.llvm.debuginfo.DILocation;

import java.io.IOException;

public class DILocationImpl extends AbstractMetadataNode implements DILocation {
    private final int line;
    private final int column;
    private final AbstractValue scope;
    private final AbstractValue inlinedAt;
    private boolean distinct;

    DILocationImpl(final int index, final int line, final int column, final AbstractValue scope, final AbstractValue inlinedAt) {
        super(index);
        this.line = line;
        this.column = column;
        this.scope = scope;
        this.inlinedAt = inlinedAt;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);

        if (distinct) {
            target.append("distinct ");
        }

        target.append("!DILocation(line: ");
        appendDecimal(target, line);

        target.append(", column: ");
        appendDecimal(target, column);

        target.append(", scope: ");
        scope.appendTo(target);

        if (inlinedAt != null) {
            target.append(", inlinedAt: ");
            inlinedAt.appendTo(target);
        }

        target.append(')');
        return appendTrailer(target);
    }

    public DILocation comment(final String comment) {
        return (DILocation) super.comment(comment);
    }

    public DILocation distinct(final boolean distinct) {
        this.distinct = distinct;
        return this;
    }
}

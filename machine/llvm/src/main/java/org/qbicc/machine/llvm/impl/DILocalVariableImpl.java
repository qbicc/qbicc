package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.EnumSet;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.llvm.debuginfo.DIFlags;
import org.qbicc.machine.llvm.debuginfo.DILocalVariable;

final class DILocalVariableImpl extends AbstractMetadataNode implements DILocalVariable {
    private final String name;
    private final AbstractValue type;
    private final AbstractValue scope;
    private final AbstractValue file;
    private final int line;
    private final int align;
    private int argument = -1;
    private EnumSet<DIFlags> flags = EnumSet.noneOf(DIFlags.class);

    DILocalVariableImpl(int index, String name, AbstractValue type, AbstractValue scope, AbstractValue file, int line, int align) {
        super(index);
        this.name = name;
        this.type = type;
        this.scope = scope;
        this.file = file;
        this.line = line;
        this.align = align;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);

        target.append("!DILocalVariable(name: ");
        appendEscapedString(target, name);

        target.append(", type: ");
        type.appendTo(target);
        target.append(", align: ");
        appendDecimal(target, align);

        if (scope != null) {
            target.append(", scope: ");
            scope.appendTo(target);
        }

        if (file != null) {
            target.append(", file: ");
            file.appendTo(target);
            target.append(", line: ");
            appendDecimal(target, line);
        }

        if (argument != -1) {
            target.append(", arg: ");
            appendDecimal(target, argument);
        }

        if (!flags.isEmpty()) {
            target.append(", flags: ");
            appendDiFlags(target, flags);
        }

        target.append(')');
        return appendTrailer(target);
    }

    public DILocalVariable comment(final String comment) {
        return (DILocalVariable) super.comment(comment);
    }

    public DILocalVariable argument(int index) {
        argument = index;
        return this;
    }

    public DILocalVariable flags(EnumSet<DIFlags> flags) {
        this.flags = Assert.checkNotNullParam("flags", flags);
        return this;
    }
}

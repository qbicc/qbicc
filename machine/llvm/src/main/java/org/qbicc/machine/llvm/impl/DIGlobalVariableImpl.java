package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.EnumSet;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.llvm.debuginfo.DIFlags;
import org.qbicc.machine.llvm.debuginfo.DIGlobalVariable;

final class DIGlobalVariableImpl extends AbstractMetadataNode implements DIGlobalVariable {
    private final String name;
    private final AbstractValue type;
    private final AbstractValue scope;
    private final AbstractValue file;
    private final int line;
    private final int align;
    private int argument = -1;
    private EnumSet<DIFlags> flags = EnumSet.noneOf(DIFlags.class);
    private boolean isDefinition;
    private boolean isLocal;

    DIGlobalVariableImpl(int index, String name, AbstractValue type, AbstractValue scope, AbstractValue file, int line, int align) {
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

        target.append("!DIGlobalVariable(name: ");
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

        target.append(", isDefinition: ");
        target.append(Boolean.toString(isDefinition));
        target.append(", isLocal: ");
        target.append(Boolean.toString(isLocal));

        target.append(')');
        return appendTrailer(target);
    }

    public DIGlobalVariable comment(final String comment) {
        return (DIGlobalVariable) super.comment(comment);
    }

    public DIGlobalVariable argument(int index) {
        argument = index;
        return this;
    }

    public DIGlobalVariable flags(EnumSet<DIFlags> flags) {
        this.flags = Assert.checkNotNullParam("flags", flags);
        return this;
    }

    @Override
    public DIGlobalVariable isDefinition() {
        this.isDefinition = true;
        return this;
    }

    @Override
    public DIGlobalVariable isLocal() {
        this.isLocal = true;
        return null;
    }
}

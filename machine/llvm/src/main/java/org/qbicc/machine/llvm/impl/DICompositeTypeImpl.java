package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.EnumSet;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.debuginfo.DICompositeType;
import org.qbicc.machine.llvm.debuginfo.DIFlags;
import org.qbicc.machine.llvm.debuginfo.DITag;
import io.smallrye.common.constraint.Assert;

public class DICompositeTypeImpl extends AbstractMetadataNode implements DICompositeType {
    private final DITag tag;
    private final long size;
    private final int align;

    private AbstractValue elements;
    private AbstractValue baseType;
    private String name;
    private EnumSet<DIFlags> flags = EnumSet.noneOf(DIFlags.class);
    private AbstractValue file;
    private int line;

    DICompositeTypeImpl(final int index, final DITag tag, final long size, final int align) {
        super(index);
        this.tag = tag;
        this.size = size;
        this.align = align;
    }

    public Appendable appendTo(Appendable target) throws IOException {
        super.appendTo(target);

        target.append("!DICompositeType(tag: ");
        target.append(tag.name);
        target.append(", size: ");
        appendDecimal(target, size);
        target.append(", align: ");
        appendDecimal(target, align);

        target.append(", elements: ");
        if (elements != null) {
            elements.appendTo(target);
        } else {
            target.append("null");
        }

        if (baseType != null) {
            target.append(", baseType: ");
            baseType.appendTo(target);
        }

        if (name != null) {
            target.append(", name: ");
            appendEscapedString(target, name);
        }

        if (!flags.isEmpty()) {
            target.append(", flags: ");
            appendDiFlags(target, flags);
        }

        if (file != null) {
            target.append(", file: ");
            file.appendTo(target);
            target.append(", line: ");
            appendDecimal(target, line);
        }

        target.append(')');
        return appendTrailer(target);
    }

    public DICompositeType elements(final LLValue elements) {
        this.elements = (AbstractValue) elements;
        return this;
    }

    public DICompositeType baseType(final LLValue baseType) {
        this.baseType = (AbstractValue) baseType;
        return this;
    }

    public DICompositeType name(final String name) {
        this.name = name;
        return this;
    }

    public DICompositeType flags(final EnumSet<DIFlags> flags) {
        Assert.checkNotNullParam("flags", flags);

        this.flags = flags;
        return this;
    }

    public DICompositeType location(final LLValue file, final int line) {
        this.file = (AbstractValue) file;
        this.line = line;
        return this;
    }

    public DICompositeType comment(final String comment) {
        return (DICompositeType) super.comment(comment);
    }
}

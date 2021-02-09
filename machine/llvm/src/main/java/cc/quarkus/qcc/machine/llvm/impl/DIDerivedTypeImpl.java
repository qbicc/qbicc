package cc.quarkus.qcc.machine.llvm.impl;

import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIDerivedType;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIFlags;
import cc.quarkus.qcc.machine.llvm.debuginfo.DITag;
import io.smallrye.common.constraint.Assert;

import java.io.IOException;
import java.util.EnumSet;

public class DIDerivedTypeImpl extends AbstractMetadataNode implements DIDerivedType {
    private final DITag tag;
    private final long size;
    private final int align;

    private AbstractValue baseType;
    private String name;
    private EnumSet<DIFlags> flags = EnumSet.noneOf(DIFlags.class);
    private long offset;
    private AbstractValue file;
    private int line;

    DIDerivedTypeImpl(final int index, final DITag tag, final long size, final int align) {
        super(index);
        this.tag = tag;
        this.size = size;
        this.align = align;
    }

    public Appendable appendTo(Appendable target) throws IOException {
        super.appendTo(target);

        target.append("!DIDerivedType(tag: ");
        target.append(tag.name);
        target.append(", size: ");
        appendDecimal(target, size);
        target.append(", align: ");
        appendDecimal(target, align);

        target.append(", baseType: ");
        if (baseType != null) {
            baseType.appendTo(target);
        } else {
            target.append("null");
        }

        if (name != null) {
            target.append(", name: ");
            appendEscapedString(target, name);
        }

        if (!flags.isEmpty()) {
            target.append(", flags: ");
            appendDiFlags(target, flags);
        }

        if (offset != 0) {
            target.append(", offset: ");
            appendDecimal(target, offset);
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

    public DIDerivedType baseType(final LLValue baseType) {
        this.baseType = (AbstractValue) baseType;
        return this;
    }

    public DIDerivedType name(final String name) {
        this.name = name;
        return this;
    }

    public DIDerivedType flags(final EnumSet<DIFlags> flags) {
        Assert.checkNotNullParam("flags", flags);

        this.flags = flags;
        return this;
    }

    public DIDerivedType offset(final long offset) {
        this.offset = offset;
        return this;
    }

    public DIDerivedType location(final LLValue file, final int line) {
        this.file = (AbstractValue) file;
        this.line = line;
        return this;
    }

    public DIDerivedType comment(final String comment) {
        return (DIDerivedType) super.comment(comment);
    }
}

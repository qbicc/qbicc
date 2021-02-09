package cc.quarkus.qcc.machine.llvm.impl;

import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIBasicType;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIEncoding;

import java.io.IOException;

public class DIBasicTypeImpl extends AbstractMetadataNode implements DIBasicType {
    private final DIEncoding encoding;
    private final long size;
    private final int align;

    private String name;
    private AbstractValue file;
    private int line;

    DIBasicTypeImpl(final int index, final DIEncoding encoding, final long size, final int align) {
        super(index);
        this.encoding = encoding;
        this.size = size;
        this.align = align;
    }

    public Appendable appendTo(Appendable target) throws IOException {
        super.appendTo(target);

        target.append("!DIBasicType(encoding: ");
        target.append(encoding.name);
        target.append(", size: ");
        appendDecimal(target, size);
        target.append(", align: ");
        appendDecimal(target, align);

        if (name != null) {
            target.append(", name: ");
            appendEscapedString(target, name);
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

    public DIBasicType name(final String name) {
        this.name = name;
        return this;
    }

    public DIBasicType location(final LLValue file, final int line) {
        this.file = (AbstractValue) file;
        this.line = line;
        return this;
    }

    public DIBasicType comment(final String comment) {
        return (DIBasicType) super.comment(comment);
    }
}

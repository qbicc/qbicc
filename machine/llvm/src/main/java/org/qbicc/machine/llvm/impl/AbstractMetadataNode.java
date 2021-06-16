package org.qbicc.machine.llvm.impl;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.debuginfo.MetadataNode;

import java.io.IOException;

abstract class AbstractMetadataNode extends AbstractCommentable implements MetadataNode {
    private final String name;
    private final int index;

    AbstractMetadataNode(final int index) {
        this.name = null;
        this.index = index;
    }

    AbstractMetadataNode(final String name) {
        this.name = name;
        this.index = -1;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        if (name != null) {
            target.append('!');
            target.append(name);
            target.append(" = ");
        } else if (index >= 0) {
            target.append('!');
            appendDecimal(target, index);
            target.append(" = ");
        }
        return target;
    }

    public LLValue asRef() {
        if (index < 0)
            throw new UnsupportedOperationException("Cannot create references to named DINodes");
        return new MetadataNodeRef(index);
    }

    public MetadataNode comment(final String comment) {
        return (MetadataNode)super.comment(comment);
    }
}

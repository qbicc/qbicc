package cc.quarkus.qcc.machine.llvm.impl;

import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.debuginfo.MetadataNode;

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
        target.append('!');

        if (index >= 0)
            appendDecimal(target, index);
        else
            target.append(name);

        target.append(" = ");
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

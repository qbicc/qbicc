package org.qbicc.machine.llvm.impl;

import java.io.IOException;

public class MetadataNodeRef extends AbstractValue {
    private final int index;

    public MetadataNodeRef(final int index) {
        this.index = index;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('!');
        appendDecimal(target, index);
        return target;
    }
}

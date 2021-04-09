package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class MetadataString extends AbstractValue {
    private final String value;

    MetadataString(final String value) {
        this.value = value;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('!');
        return appendEscapedString(target, value);
    }
}

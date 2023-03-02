package org.qbicc.machine.llvm.impl;

import java.io.IOException;

/**
 *
 */
final class SourceFileImpl extends AbstractEmittable {
    private final String path;

    SourceFileImpl(final String path) {
        this.path = path;
    }

    @Override
    public Appendable appendTo(Appendable target) throws IOException {
        target.append("source_filename = ");
        appendEscapedString(target, path);
        return target;
    }
}

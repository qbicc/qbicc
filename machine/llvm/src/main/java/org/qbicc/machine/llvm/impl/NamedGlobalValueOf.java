package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class NamedGlobalValueOf extends AbstractValue {
    private final String name;

    NamedGlobalValueOf(String name) {
        super();
        this.name = LLVM.needsQuotes(name) ? LLVM.quoteString(name) : name;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return target.append('@').append(name);
    }
}

package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class NamedLocalValueOf extends AbstractValue {
    private final String name;

    NamedLocalValueOf(AbstractInstruction instruction, final String name) {
        super(instruction);
        this.name = LLVM.needsQuotes(name) ? LLVM.quoteString(name) : name;;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return target.append('%').append(name);
    }
}

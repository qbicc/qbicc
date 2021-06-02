package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.op.YieldingInstruction;

final class NamedLocalValueOf extends AbstractValue {
    private final AbstractInstruction instruction;
    private final String name;

    NamedLocalValueOf(AbstractInstruction instruction, final String name) {
        super();
        this.instruction = instruction;
        this.name = name;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return target.append('%').append(name);
    }

    @Override
    public YieldingInstruction getInstruction() {
        return (YieldingInstruction) instruction;
    }
}

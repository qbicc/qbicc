package org.qbicc.machine.llvm.impl;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.YieldingInstruction;

abstract class AbstractValue extends AbstractEmittable implements LLValue {
    private AbstractInstruction instruction;

    AbstractValue() {
    }

    AbstractValue(AbstractInstruction instruction) {
        this.instruction = instruction;
    }

    void setInstruction(final AbstractInstruction instruction) {
        YieldingInstruction instr = getInstruction();
        if (instr != instruction && instr != null) {
            throw new IllegalStateException("Already assigned to an instruction");
        }
        this.instruction = instruction;
    }

    @Override
    public YieldingInstruction getInstruction() {
        return (YieldingInstruction) instruction;
    }
}

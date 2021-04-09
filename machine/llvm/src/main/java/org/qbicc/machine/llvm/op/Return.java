package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface Return extends Instruction {
    Return meta(String name, LLValue data);

    Return comment(String comment);
}

package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface Branch extends Instruction {
    Branch meta(String name, LLValue data);

    Branch comment(String comment);
}

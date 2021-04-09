package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface Fence extends Instruction {
    Fence comment(String comment);

    Fence meta(String name, LLValue data);

    Fence syncScope(String scopeName);
}

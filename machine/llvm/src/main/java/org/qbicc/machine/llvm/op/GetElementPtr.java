package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

/**
 * Mapping of the LLVM {@code getelementptr} instruction.
 */
public interface GetElementPtr extends YieldingInstruction {
    GetElementPtr comment(String comment);
    GetElementPtr meta(String name, LLValue data);
    GetElementPtr arg(boolean inRange, LLValue type, LLValue index);
}

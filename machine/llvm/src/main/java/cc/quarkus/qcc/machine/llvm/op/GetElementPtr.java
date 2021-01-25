package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 * Mapping of the LLVM {@code getelementptr} instruction.
 */
public interface GetElementPtr extends YieldingInstruction {
    GetElementPtr comment(String comment);
    GetElementPtr meta(String name, LLValue data);
    GetElementPtr arg(boolean inRange, LLValue type, LLValue index);
}

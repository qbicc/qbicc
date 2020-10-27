package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface Fence extends Instruction {
    Fence comment(String comment);

    Fence meta(String name, LLValue data);

    Fence syncScope(String scopeName);
}

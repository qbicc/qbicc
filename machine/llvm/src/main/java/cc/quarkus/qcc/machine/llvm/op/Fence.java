package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface Fence extends Instruction {
    Fence comment(String comment);

    Fence meta(String name, Value data);

    Fence syncScope(String scopeName);
}

package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface Return extends Instruction {
    Return meta(String name, Value data);

    Return comment(String comment);
}

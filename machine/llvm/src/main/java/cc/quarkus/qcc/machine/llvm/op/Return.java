package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface Return extends Instruction {
    Return meta(String name, LLValue data);

    Return comment(String comment);
}

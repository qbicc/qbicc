package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface Branch extends Instruction {
    Branch meta(String name, LLValue data);

    Branch comment(String comment);
}

package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface Branch extends Instruction {
    Branch meta(String name, Value data);

    Branch comment(String comment);
}

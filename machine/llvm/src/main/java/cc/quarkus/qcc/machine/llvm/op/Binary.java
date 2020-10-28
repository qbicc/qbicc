package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface Binary extends YieldingInstruction {
    Binary comment(String comment);

    Binary meta(String name, LLValue data);
}

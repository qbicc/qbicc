package cc.quarkus.qcc.machine.llvm;

import cc.quarkus.qcc.machine.llvm.op.YieldingInstruction;

/**
 *
 */
public interface Global extends YieldingInstruction {
    Global meta(String name, LLValue data);

    Global value(LLValue value);
}

package cc.quarkus.qcc.machine.llvm;

import cc.quarkus.qcc.machine.llvm.op.YieldingInstruction;

/**
 *
 */
public interface Global extends YieldingInstruction {
    Global meta(String name, Value data);

    Global value(Value value);
}

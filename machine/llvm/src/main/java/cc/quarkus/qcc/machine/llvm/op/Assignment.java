package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface Assignment extends YieldingInstruction {
    Assignment comment(String comment);

    Assignment meta(String name, Value data);
}

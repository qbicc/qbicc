package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface Binary extends YieldingInstruction {
    Binary comment(String comment);

    Binary meta(String name, Value data);
}

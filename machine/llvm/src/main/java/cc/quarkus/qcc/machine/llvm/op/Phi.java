package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.BasicBlock;
import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface Phi extends YieldingInstruction {
    Phi meta(String name, Value data);

    Phi comment(String comment);

    Phi item(Value data, BasicBlock incoming);
}

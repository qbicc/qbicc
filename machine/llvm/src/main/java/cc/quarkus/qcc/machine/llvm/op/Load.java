package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface Load extends YieldingInstruction {
    Load comment(String comment);

    Load meta(String name, Value data);

    Load align(int alignment);

    Load atomic(OrderingConstraint constraint);

    Load atomic(OrderingConstraint constraint, String syncScope);

    Load volatile_();
}

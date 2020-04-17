package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface Store extends Instruction {
    Store meta(String name, Value data);

    Store align(int alignment);

    Store atomic(OrderingConstraint constraint);

    Store atomic(OrderingConstraint constraint, String syncScope);

    Store volatile_();
}

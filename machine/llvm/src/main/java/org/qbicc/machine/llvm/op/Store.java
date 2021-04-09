package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface Store extends Instruction {
    Store meta(String name, LLValue data);

    Store comment(String comment);

    Store align(int alignment);

    Store atomic(OrderingConstraint constraint);

    Store atomic(OrderingConstraint constraint, String syncScope);

    Store volatile_();
}

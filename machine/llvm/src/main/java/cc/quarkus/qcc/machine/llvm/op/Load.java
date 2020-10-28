package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface Load extends YieldingInstruction {
    Load comment(String comment);

    Load meta(String name, LLValue data);

    Load align(int alignment);

    Load atomic(OrderingConstraint constraint);

    Load atomic(OrderingConstraint constraint, String syncScope);

    Load volatile_();
}

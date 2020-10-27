package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface AtomicRmwInstruction extends YieldingInstruction {
    AtomicRmwInstruction comment(String comment);

    AtomicRmwInstruction meta(String name, LLValue data);

    AtomicRmwInstruction volatile_();

    AtomicRmwInstruction syncScope(String scopeName);

    AtomicRmwInstruction xchg();

    AtomicRmwInstruction add();

    AtomicRmwInstruction sub();

    AtomicRmwInstruction and();

    AtomicRmwInstruction nand();

    AtomicRmwInstruction or();

    AtomicRmwInstruction xor();

    AtomicRmwInstruction max();

    AtomicRmwInstruction min();

    AtomicRmwInstruction umax();

    AtomicRmwInstruction umin();

    AtomicRmwInstruction fadd();

    AtomicRmwInstruction fsub();

    AtomicRmwInstruction ordering(OrderingConstraint order);
}

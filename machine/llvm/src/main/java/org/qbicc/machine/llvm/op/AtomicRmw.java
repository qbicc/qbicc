package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface AtomicRmw extends YieldingInstruction {
    AtomicRmw comment(String comment);

    AtomicRmw meta(String name, LLValue data);

    AtomicRmw align(int alignment);

    AtomicRmw volatile_();

    AtomicRmw syncScope(String scopeName);

    AtomicRmw xchg();

    AtomicRmw add();

    AtomicRmw sub();

    AtomicRmw and();

    AtomicRmw nand();

    AtomicRmw or();

    AtomicRmw xor();

    AtomicRmw max();

    AtomicRmw min();

    AtomicRmw umax();

    AtomicRmw umin();

    AtomicRmw fadd();

    AtomicRmw fsub();

    AtomicRmw ordering(OrderingConstraint order);
}

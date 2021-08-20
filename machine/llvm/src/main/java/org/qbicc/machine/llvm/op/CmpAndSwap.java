package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

public interface CmpAndSwap extends YieldingInstruction {
    CmpAndSwap comment(String comment);
    CmpAndSwap meta(String name, LLValue data);
}

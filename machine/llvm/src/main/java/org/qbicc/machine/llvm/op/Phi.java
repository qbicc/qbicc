package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLBasicBlock;
import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface Phi extends YieldingInstruction {
    Phi meta(String name, LLValue data);

    Phi comment(String comment);

    Phi item(LLValue data, LLBasicBlock incoming);
}

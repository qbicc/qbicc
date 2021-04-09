package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLBasicBlock;
import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface Switch extends Instruction {
    Switch meta(String name, LLValue data);

    Switch comment(String comment);

    Case case_(LLValue value, LLBasicBlock target);

    interface Case {
        Case case_(LLValue value, LLBasicBlock target);
    }
}

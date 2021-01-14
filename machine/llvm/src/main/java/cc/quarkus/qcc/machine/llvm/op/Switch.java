package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLBasicBlock;
import cc.quarkus.qcc.machine.llvm.LLValue;

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

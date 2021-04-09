package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface Assignment extends YieldingInstruction {
    Assignment comment(String comment);

    Assignment meta(String name, LLValue data);
}

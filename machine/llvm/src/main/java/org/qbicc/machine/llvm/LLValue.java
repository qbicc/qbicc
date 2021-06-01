package org.qbicc.machine.llvm;

import org.qbicc.machine.llvm.op.YieldingInstruction;

/**
 *
 */
public interface LLValue {
    default YieldingInstruction getInstruction() {
        return null;
    }
}

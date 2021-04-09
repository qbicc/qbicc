package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface Binary extends YieldingInstruction {
    Binary comment(String comment);

    Binary meta(String name, LLValue data);
}

package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface Alloca extends YieldingInstruction {
    Alloca elements(LLValue type, LLValue count);

    Alloca align(LLValue align);

    Alloca comment(String comment);

    Alloca meta(String name, LLValue data);
}

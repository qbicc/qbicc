package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface ExactBinary extends Binary {
    ExactBinary comment(String comment);

    ExactBinary meta(String name, LLValue data);

    ExactBinary exact();
}

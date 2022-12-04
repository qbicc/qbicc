package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface HasArguments {
    Argument arg(LLValue type, LLValue value);

    interface Argument {
        Argument attribute(LLValue attribute);

        Argument arg(LLValue type, LLValue value);
    }
}

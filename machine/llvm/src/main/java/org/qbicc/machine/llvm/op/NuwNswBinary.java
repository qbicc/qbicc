package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface NuwNswBinary extends Binary {
    NuwNswBinary comment(String comment);

    NuwNswBinary meta(String name, LLValue data);

    NuwNswBinary nuw();

    NuwNswBinary nsw();
}

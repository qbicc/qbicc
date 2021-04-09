package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.Metable;
import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface Instruction extends Metable {
    Instruction comment(String comment);

    Instruction meta(String name, LLValue data);
}

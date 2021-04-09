package org.qbicc.machine.llvm.op;

import java.util.Set;

import org.qbicc.machine.llvm.FastMathFlag;
import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface Select extends YieldingInstruction {
    Select meta(String name, LLValue data);

    Select comment(String comment);

    Select withFlags(Set<FastMathFlag> flagSet);
}

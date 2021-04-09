package org.qbicc.machine.llvm.op;

import java.util.Set;

import org.qbicc.machine.llvm.FastMathFlag;
import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface FastMathUnary extends YieldingInstruction {
    FastMathUnary comment(String comment);

    FastMathUnary meta(String name, LLValue data);

    FastMathUnary withFlags(Set<FastMathFlag> flags);
}

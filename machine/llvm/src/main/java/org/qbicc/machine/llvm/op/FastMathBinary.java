package org.qbicc.machine.llvm.op;

import java.util.Set;

import org.qbicc.machine.llvm.FastMathFlag;
import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface FastMathBinary extends Binary {
    FastMathBinary comment(String comment);

    FastMathBinary meta(String name, LLValue data);

    FastMathBinary withFlags(Set<FastMathFlag> flags);
}

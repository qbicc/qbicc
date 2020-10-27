package cc.quarkus.qcc.machine.llvm.op;

import java.util.Set;

import cc.quarkus.qcc.machine.llvm.FastMathFlag;
import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface FastMathUnary extends YieldingInstruction {
    FastMathUnary comment(String comment);

    FastMathUnary meta(String name, LLValue data);

    FastMathUnary withFlags(Set<FastMathFlag> flags);
}

package cc.quarkus.qcc.machine.llvm.op;

import java.util.Set;

import cc.quarkus.qcc.machine.llvm.FastMathFlag;
import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface FastMathUnary extends YieldingInstruction {
    FastMathUnary comment(String comment);

    FastMathUnary meta(String name, Value data);

    FastMathUnary withFlags(Set<FastMathFlag> flags);
}

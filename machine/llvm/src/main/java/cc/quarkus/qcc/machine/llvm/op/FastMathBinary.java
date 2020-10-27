package cc.quarkus.qcc.machine.llvm.op;

import java.util.Set;

import cc.quarkus.qcc.machine.llvm.FastMathFlag;
import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface FastMathBinary extends Binary {
    FastMathBinary comment(String comment);

    FastMathBinary meta(String name, LLValue data);

    FastMathBinary withFlags(Set<FastMathFlag> flags);
}

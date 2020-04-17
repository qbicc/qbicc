package cc.quarkus.qcc.machine.llvm.op;

import java.util.Set;

import cc.quarkus.qcc.machine.llvm.FastMathFlag;
import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface Select extends YieldingInstruction {
    Select meta(String name, Value data);

    Select withFlags(Set<FastMathFlag> flagSet);
}

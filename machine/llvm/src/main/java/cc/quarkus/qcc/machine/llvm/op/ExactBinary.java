package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface ExactBinary extends Binary {
    ExactBinary comment(String comment);

    ExactBinary meta(String name, Value data);

    ExactBinary exact();
}

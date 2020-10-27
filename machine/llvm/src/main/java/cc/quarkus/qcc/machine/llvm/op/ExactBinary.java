package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface ExactBinary extends Binary {
    ExactBinary comment(String comment);

    ExactBinary meta(String name, LLValue data);

    ExactBinary exact();
}

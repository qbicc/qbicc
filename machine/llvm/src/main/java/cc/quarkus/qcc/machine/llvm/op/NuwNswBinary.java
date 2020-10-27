package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface NuwNswBinary extends Binary {
    NuwNswBinary comment(String comment);

    NuwNswBinary meta(String name, LLValue data);

    NuwNswBinary nuw();

    NuwNswBinary nsw();
}

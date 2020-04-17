package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface NuwNswBinary extends Binary {
    NuwNswBinary meta(String name, Value data);

    NuwNswBinary nuw();

    NuwNswBinary nsw();
}

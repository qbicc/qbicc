package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.Metable;
import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface Instruction extends Metable {
    Instruction meta(String name, Value data);
}

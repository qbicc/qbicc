package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.Metable;
import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface Instruction extends Metable {
    Instruction comment(String comment);

    Instruction meta(String name, LLValue data);
}

package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 * Mapping of the LLVM {@code extractvalue} instruction.
 */
public interface ExtractValue extends YieldingInstruction {
    ExtractValue comment(String comment);
    ExtractValue meta(String name, LLValue data);
    ExtractValue arg(LLValue index);
}

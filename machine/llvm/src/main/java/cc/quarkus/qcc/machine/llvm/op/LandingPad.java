package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface LandingPad extends YieldingInstruction {
    LandingPad comment(String comment);

    LandingPad meta(String name, LLValue data);

    LandingPad cleanup();

    LandingPad catch_(LLValue type, LLValue value);

    LandingPad filter(LLValue type, LLValue value);
}

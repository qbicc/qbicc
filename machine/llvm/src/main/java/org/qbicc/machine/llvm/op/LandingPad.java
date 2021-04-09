package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

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

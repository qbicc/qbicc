package org.qbicc.machine.llvm;

import org.qbicc.machine.llvm.impl.LLVM;

/**
 *
 */
public final class ParameterAttributes {
    private ParameterAttributes() {}

    public static final LLValue signext = LLVM.flagAttribute("signext");
    public static final LLValue zeroext = LLVM.flagAttribute("zeroext");
    public static final LLValue inreg = LLVM.flagAttribute("inreg");

    public static LLValue elementtype(LLValue type) {
        return LLVM.argumentAttribute("elementtype", type);
    }
}

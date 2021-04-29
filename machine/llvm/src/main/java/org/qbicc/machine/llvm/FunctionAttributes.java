package org.qbicc.machine.llvm;

import org.qbicc.machine.llvm.impl.LLVM;

/**
 *
 */
public final class FunctionAttributes {
    private FunctionAttributes() {}

    public static final LLValue uwtable = LLVM.flagAttribute("uwtable");
}


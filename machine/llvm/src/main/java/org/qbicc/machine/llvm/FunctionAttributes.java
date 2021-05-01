package org.qbicc.machine.llvm;

import org.qbicc.machine.llvm.impl.LLVM;

/**
 *
 */
public final class FunctionAttributes {
    private FunctionAttributes() {}

    public static final LLValue alwaysinline = LLVM.flagAttribute("alwaysinline");
    public static final LLValue gcLeafFunction = LLVM.flagAttribute("\"gc-leaf-function\"");
    public static final LLValue uwtable = LLVM.flagAttribute("uwtable");

    public static LLValue framePointer(String val) {
        return LLVM.valueAttribute("\"frame-pointer\"", LLVM.quoteString(val));
    }
}


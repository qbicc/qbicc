package org.qbicc.machine.llvm;

/**
 *
 */
@SuppressWarnings("SpellCheckingInspection")
public enum AsmFlag {
    SIDE_EFFECT("sideeffect"),
    ALIGN_STACK("alignstack"),
    INTEL_DIALECT("inteldialect"),
    UNWIND("unwind"),
    ;

    private final String llvmString;

    AsmFlag(String llvmString) {
        this.llvmString = llvmString;
    }

    public String getLlvmString() {
        return llvmString;
    }
}

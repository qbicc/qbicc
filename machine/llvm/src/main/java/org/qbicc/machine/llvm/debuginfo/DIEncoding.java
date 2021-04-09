package org.qbicc.machine.llvm.debuginfo;

public enum DIEncoding {
    Address("DW_ATE_address"),
    Boolean("DW_ATE_boolean"),
    ComplexFloat("DW_ATE_complex_float"),
    Float("DW_ATE_float"),
    Signed("DW_ATE_signed"),
    SignedChar("DW_ATE_signed_char"),
    Unsigned("DW_ATE_unsigned"),
    UnsignedChar("DW_ATE_unsigned_char");

    public final String name;

    DIEncoding(String name) {
        this.name = name;
    }
}

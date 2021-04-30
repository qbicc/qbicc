package org.qbicc.machine.llvm.stackmap;

/**
 *
 */
public enum LocationType {
    // ordering must not change
    Register,
    Direct,
    Indirect,
    Constant,
    ;

    private static final LocationType[] values = { Register, Direct, Indirect, Constant, Constant };

    public int getEncoding() {
        return ordinal() + 1;
    }

    public static LocationType forEncoding(int encoded) {
        return values[encoded - 1];
    }
}

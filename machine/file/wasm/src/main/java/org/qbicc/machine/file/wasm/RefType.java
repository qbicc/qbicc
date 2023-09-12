package org.qbicc.machine.file.wasm;

/**
 *
 */
public
enum RefType implements ValType {
    funcref(0x70),
    externref(0x6f),
    ;
    private final int val;

    RefType(int val) {
        this.val = val;
    }

    public int byteValue() {
        return val;
    }

    public static org.qbicc.machine.file.wasm.RefType forByteValue(int val) {
        return switch (val) {
            case 0x70 -> funcref;
            case 0x6f -> externref;
            default -> throw new IllegalArgumentException("Invalid byte value " + Integer.toHexString(val));
        };
    }
}

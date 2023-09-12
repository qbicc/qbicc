package org.qbicc.machine.file.wasm;

/**
 *
 */
public
enum VecType implements ValType {
    v128(0x7b),
    ;
    private final int val;

    VecType(int val) {
        this.val = val;
    }

    public int byteValue() {
        return val;
    }

    public static org.qbicc.machine.file.wasm.VecType forByteValue(int val) {
        return switch (val) {
            case 0x7b -> v128;
            default -> throw new IllegalArgumentException("Invalid byte value " + Integer.toHexString(val));
        };
    }
}

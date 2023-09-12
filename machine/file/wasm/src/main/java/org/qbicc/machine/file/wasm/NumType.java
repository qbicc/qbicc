package org.qbicc.machine.file.wasm;

/**
 *
 */
public
enum NumType implements ValType {
    i32(0x7f),
    i64(0x7e),
    f32(0x7d),
    f64(0x7c),
    ;
    private final int val;

    NumType(int val) {
        this.val = val;
    }

    public int byteValue() {
        return val;
    }

    public static org.qbicc.machine.file.wasm.NumType forByteValue(int val) {
        return switch (val) {
            case 0x7f -> i32;
            case 0x7e -> i64;
            case 0x7d -> f32;
            case 0x7c -> f64;
            default -> throw new IllegalArgumentException("Invalid byte value " + Integer.toHexString(val));
        };
    }
}

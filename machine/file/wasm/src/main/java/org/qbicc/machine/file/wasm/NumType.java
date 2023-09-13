package org.qbicc.machine.file.wasm;

import java.util.List;

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
    private final List<ValType> list;
    private final FuncType funcType;

    NumType(int val) {
        this.val = val;
        list = List.of(this);
        funcType = new FuncType(List.of(), list);
    }

    public int byteValue() {
        return val;
    }

    @Override
    public List<ValType> asList() {
        return list;
    }

    @Override
    public FuncType asFuncTypeReturning() {
        return funcType;
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

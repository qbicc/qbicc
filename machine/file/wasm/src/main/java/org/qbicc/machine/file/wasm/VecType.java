package org.qbicc.machine.file.wasm;

import java.util.List;

/**
 *
 */
public
enum VecType implements ValType {
    v128(0x7b),
    ;
    private final int val;
    private final List<ValType> list;
    private final FuncType funcType;

    VecType(int val) {
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

    public static org.qbicc.machine.file.wasm.VecType forByteValue(int val) {
        return switch (val) {
            case 0x7b -> v128;
            default -> throw new IllegalArgumentException("Invalid byte value " + Integer.toHexString(val));
        };
    }
}

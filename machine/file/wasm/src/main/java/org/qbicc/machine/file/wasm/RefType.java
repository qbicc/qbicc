package org.qbicc.machine.file.wasm;

import java.util.List;

/**
 *
 */
public
enum RefType implements ValType {
    funcref(0x70),
    externref(0x6f),
    ;
    private final int val;
    private final List<ValType> list;
    private final FuncType funcType;

    RefType(int val) {
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

    public static org.qbicc.machine.file.wasm.RefType forByteValue(int val) {
        return switch (val) {
            case 0x70 -> funcref;
            case 0x6f -> externref;
            default -> throw new IllegalArgumentException("Invalid byte value " + Integer.toHexString(val));
        };
    }
}

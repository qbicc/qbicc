package org.qbicc.machine.file.wasm;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * All simple value types.
 */
public sealed interface ValType extends Type permits NumType, VecType, RefType {
    int byteValue();

    List<ValType> asList();

    FuncType asFuncTypeReturning();

    static ValType forByteValue(int val) {
        return switch (val) {
            case 0x7f -> NumType.i32;
            case 0x7e -> NumType.i64;
            case 0x7d -> NumType.f32;
            case 0x7c -> NumType.f64;
            case 0x7b -> VecType.v128;
            case 0x70 -> RefType.funcref;
            case 0x6f -> RefType.externref;
            default -> throw new IllegalArgumentException("Invalid byte value " + Integer.toHexString(val));
        };
    }

    static ValType[] values() {
        NumType[] numTypes = NumType.values();
        VecType[] vecTypes = VecType.values();
        RefType[] refTypes = RefType.values();
        ValType[] values = Arrays.copyOf(numTypes, numTypes.length + vecTypes.length + refTypes.length, ValType[].class);
        System.arraycopy(vecTypes, 0, values, numTypes.length, vecTypes.length);
        System.arraycopy(refTypes, 0, values, numTypes.length + vecTypes.length, refTypes.length);
        return values;
    }

    Comparator<ValType> CMP = (o1, o2) -> Integer.compareUnsigned(o1.byteValue(), o2.byteValue());
}

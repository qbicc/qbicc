package org.qbicc.machine.file.wasm;

import java.util.Arrays;
import java.util.List;

/**
 * All simple value types.
 */
public sealed interface ValType extends Type permits NumType, VecType, RefType {
    int byteValue();

    List<ValType> asList();

    FuncType asFuncTypeReturning();

    static boolean isValidByteValue(int val) {
        return switch (val) {
            case 0x7f, 0x7e, 0x7d, 0x7c, 0x7b, 0x70, 0x6f -> true;
            default -> false;
        };
    }

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

    static int compare(ValType a, ValType b) {
        return Integer.compareUnsigned(a.byteValue(), b.byteValue());
    }

    static int compare(List<ValType> a, List<ValType> b) {
        int asz = a.size();
        int bsz = b.size();
        int minSize = Math.min(asz, bsz);
        for (int idx = 0; idx < minSize; idx ++) {
            int res = compare(a.get(idx), b.get(idx));
            if (res != 0) {
                return res;
            }
        }
        return Integer.compare(asz, bsz);
    }
}

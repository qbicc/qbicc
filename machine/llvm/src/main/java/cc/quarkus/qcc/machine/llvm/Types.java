package cc.quarkus.qcc.machine.llvm;

import cc.quarkus.qcc.machine.llvm.impl.LLVM;

/**
 *
 */
public final class Types {
    private Types() {}

    public static final Value i1 = LLVM.i1;
    public static final Value i8 = LLVM.i8;
    public static final Value i16 = LLVM.i16;
    public static final Value i24 = LLVM.i24;
    public static final Value i32 = LLVM.i32;
    public static final Value i64 = LLVM.i64;
    public static final Value i128 = LLVM.i128;

    public static final Value float16 = LLVM.float16;
    public static final Value float32 = LLVM.float32;
    public static final Value float64 = LLVM.float64;
    public static final Value float128 = LLVM.float128;

    public static Value ptrTo(Value type) {
        return LLVM.ptrTo(type, 0);
    }

    public static Value ptrTo(Value type, int addrSpace) {
        return LLVM.ptrTo(type, addrSpace);
    }

    public static Value array(int dimension, Value elementType) {
        return LLVM.array(dimension, elementType);
    }

    public static Value vector(boolean vscale, int dimension, Value elementType) {
        return LLVM.vector(vscale, dimension, elementType);
    }


}

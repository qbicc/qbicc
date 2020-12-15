package cc.quarkus.qcc.machine.llvm;

import java.util.List;

import cc.quarkus.qcc.machine.llvm.impl.LLVM;

/**
 *
 */
public final class Types {
    private Types() {}

    public static final LLValue i1 = LLVM.i1;
    public static final LLValue i8 = LLVM.i8;
    public static final LLValue i16 = LLVM.i16;
    public static final LLValue i24 = LLVM.i24;
    public static final LLValue i32 = LLVM.i32;
    public static final LLValue i64 = LLVM.i64;
    public static final LLValue i128 = LLVM.i128;

    public static final LLValue float16 = LLVM.float16;
    public static final LLValue float32 = LLVM.float32;
    public static final LLValue float64 = LLVM.float64;
    public static final LLValue float128 = LLVM.float128;

    public static final LLValue void_ = LLVM.void_;

    public static LLValue ptrTo(LLValue type) {
        return LLVM.ptrTo(type, 0);
    }

    public static LLValue ptrTo(LLValue type, int addrSpace) {
        return LLVM.ptrTo(type, addrSpace);
    }

    public static LLValue array(int dimension, LLValue elementType) {
        return LLVM.array(dimension, elementType);
    }

    public static LLValue vector(boolean vscale, int dimension, LLValue elementType) {
        return LLVM.vector(vscale, dimension, elementType);
    }

    public static LLValue function(final LLValue returnType, final List<LLValue> argTypes) {
        return LLVM.function(returnType, argTypes);
    }

    public static LLStruct struct() {
        return LLVM.struct();
    }
}

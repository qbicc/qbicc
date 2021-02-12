package cc.quarkus.qcc.machine.llvm;

import java.util.List;

import cc.quarkus.qcc.machine.llvm.impl.LLVM;

/**
 *
 */
public final class Values {

    private Values() {}

    public static final LLValue ZERO = LLVM.ZERO;
    public static final LLValue TRUE = LLVM.TRUE;
    public static final LLValue FALSE = LLVM.FALSE;
    public static final LLValue NULL = LLVM.NULL;

    public static final LLValue zeroinitializer = LLVM.zeroinitializer;

    public static LLValue bitcastConstant(LLValue value, LLValue fromType, LLValue toType) { return LLVM.bitcastConstant(value, fromType, toType); }

    public static LLValue intConstant(int val) {
        return LLVM.intConstant(val);
    }

    public static LLValue intConstant(long val) {
        return LLVM.intConstant(val);
    }

    public static LLValue floatConstant(float val) {
        return LLVM.floatConstant(val);
    }

    public static LLValue floatConstant(double val) {
        return LLVM.floatConstant(val);
    }

    public static LLValue global(String name) {
        return LLVM.global(name);
    }

    public static Array array(LLValue elementType) {
        return LLVM.array(elementType);
    }

    public static Struct struct() {
        return LLVM.struct();
    }

    public static LLValue metadataString(String value) {
        return LLVM.metadataString(value);
    }
}

package cc.quarkus.qcc.machine.llvm;

import cc.quarkus.qcc.machine.llvm.impl.LLVM;

/**
 *
 */
public final class Values {
    private Values() {}

    public static final LLValue ZERO = LLVM.ZERO;
    public static final LLValue TRUE = LLVM.TRUE;
    public static final LLValue FALSE = LLVM.FALSE;

    public static LLValue intConstant(int val) {
        return LLVM.intConstant(val);
    }

    public static LLValue intConstant(long val) {
        return LLVM.intConstant(val);
    }
}

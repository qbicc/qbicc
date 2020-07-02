package cc.quarkus.qcc.machine.llvm;

import cc.quarkus.qcc.machine.llvm.impl.LLVM;

/**
 *
 */
public final class Values {
    private Values() {}

    public static final Value ZERO = LLVM.ZERO;
    public static final Value TRUE = LLVM.TRUE;
    public static final Value FALSE = LLVM.FALSE;

    public static Value intConstant(int val) {
        return LLVM.intConstant(val);
    }

    public static Value intConstant(long val) {
        return LLVM.intConstant(val);
    }
}

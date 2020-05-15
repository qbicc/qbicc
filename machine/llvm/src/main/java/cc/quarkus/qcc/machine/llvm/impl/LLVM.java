package cc.quarkus.qcc.machine.llvm.impl;

import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public final class LLVM {

    private LLVM() {}

    public static Module newModule() {
        return new ModuleImpl();
    }

    public static final Value i1 = new SingleWord("i1");
    public static final Value i8 = new SingleWord("i8");
    public static final Value i16 = new SingleWord("i16");
    public static final Value i24 = new SingleWord("i24");
    public static final Value i32 = new SingleWord("i32");
    public static final Value i64 = new SingleWord("i64");
    public static final Value i128 = new SingleWord("i128");

    public static final Value float16 = new SingleWord("half");
    public static final Value float32 = new SingleWord("float");
    public static final Value float64 = new SingleWord("double");
    public static final Value float128 = new SingleWord("fp128");

    public static final Value ZERO = new IntConstant(0);

    public static Value ptrTo(Value type, int addrSpace) {
        return new PointerTo((AbstractValue) type, addrSpace);
    }

    public static Value array(int dimension, Value elementType) {
        return new ArrayOf(dimension, (AbstractValue) elementType);
    }

    public static Value vector(final boolean vscale, final int dimension, final Value elementType) {
        return new VectorOf(dimension, (AbstractValue) elementType, vscale);
    }

    public static Value intConstant(int val) {
        return new IntConstant(val);
    }
}

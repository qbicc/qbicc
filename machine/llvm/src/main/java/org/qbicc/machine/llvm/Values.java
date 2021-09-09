package org.qbicc.machine.llvm;

import org.qbicc.machine.llvm.debuginfo.DIExpression;
import org.qbicc.machine.llvm.impl.LLVM;

/**
 *
 */
public final class Values {

    private Values() {}

    public static final LLValue ZERO = LLVM.ZERO;
    public static final LLValue TRUE = LLVM.TRUE;
    public static final LLValue FALSE = LLVM.FALSE;
    public static final LLValue NULL = LLVM.NULL;
    public static final LLValue UNDEF = LLVM.UNDEF;

    public static final LLValue zeroinitializer = LLVM.zeroinitializer;

    public static LLValue bitcastConstant(LLValue value, LLValue fromType, LLValue toType) {
        return LLVM.bitcastConstant(value, fromType, toType);
    }

    public static LLValue gepConstant(LLValue type, LLValue ptrType, LLValue pointer, LLValue ... args) {
        return LLVM.gepConstant(type, ptrType, pointer, args);
    }

    public static LLValue addrspacecastConstant(LLValue value, LLValue fromType, LLValue toType) {
        return LLVM.addrspacecastConstant(value, fromType, toType);
    }

    public static LLValue inttoptrConstant(LLValue value, LLValue fromType, LLValue toType) {
        return LLVM.inttoptrConstant(value, fromType, toType);
    }

    public static LLValue ptrtointConstant(LLValue value, LLValue fromType, LLValue toType) {
        return LLVM.ptrtointConstant(value, fromType, toType);
    }

    public static LLValue functionParameterConstant(String name) {
        return LLVM.functionParameterConstant(name);
    }

    public static DIExpression diExpression() {
        return LLVM.diExpression();
    }

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

    public static LLValue byteArray(byte[] contents) {
        return LLVM.byteArray(contents);
    }

    public static Struct struct() {
        return LLVM.struct();
    }

    public static LLValue metadataString(String value) {
        return LLVM.metadataString(value);
    }
}

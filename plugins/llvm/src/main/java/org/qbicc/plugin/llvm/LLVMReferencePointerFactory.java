package org.qbicc.plugin.llvm;

import org.qbicc.machine.llvm.LLBuilder;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Values;

import static org.qbicc.machine.llvm.impl.LLVM.i8;
import static org.qbicc.machine.llvm.impl.LLVM.ptrTo;

public interface LLVMReferencePointerFactory {
    LLValue makeReferencePointer();
    LLValue makeRawPointer();

    LLValue cast(LLValue value, LLValue fromType, LLValue toType);

    void buildCastPtrToRef(LLBuilder builder, LLValue val);

    void buildCastRefToPtr(LLBuilder builder, LLValue val);

    LLVMReferencePointerFactory COLLECTED = new LLVMReferencePointerFactory() {
        LLValue ref = ptrTo(i8, 1);
        LLValue raw = ptrTo(i8, 0);

        @Override
        public LLValue makeReferencePointer() {
            return ref;
        }

        @Override
        public LLValue makeRawPointer() {
            return raw;
        }

        @Override
        public LLValue cast(LLValue input, LLValue fromType, LLValue toType) {
            return Values.addrspacecastConstant(input, fromType, toType);
        }

        @Override
        public void buildCastPtrToRef(LLBuilder builder, LLValue val) {
            builder.ret(
                ref,
                builder.addrspacecast(raw, val, ref).asLocal("ref")
            );
        }

        @Override
        public void buildCastRefToPtr(LLBuilder builder, LLValue val) {
            builder.ret(
                raw,
                builder.addrspacecast(ref, val, raw).asLocal("ptr")
            );
        }
    };

    LLVMReferencePointerFactory SIMPLE = new LLVMReferencePointerFactory() {
        LLValue ptr = ptrTo(i8, 0);
        @Override
        public LLValue makeReferencePointer() {
            return ptr;
        }

        @Override
        public LLValue makeRawPointer() {
            return ptr;
        }

        @Override
        public LLValue cast(LLValue input, LLValue fromType, LLValue toType) {
            return Values.bitcastConstant(input, fromType, toType);
        }

        @Override
        public void buildCastPtrToRef(LLBuilder builder, LLValue val) {
            builder.ret(ptr, val);
        }

        @Override
        public void buildCastRefToPtr(LLBuilder builder, LLValue val) {
            builder.ret(ptr, val);
        }
    };
}

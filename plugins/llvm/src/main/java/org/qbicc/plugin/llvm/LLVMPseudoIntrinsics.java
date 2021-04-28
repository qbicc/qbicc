package org.qbicc.plugin.llvm;

import org.qbicc.machine.llvm.FunctionDefinition;
import org.qbicc.machine.llvm.LLBasicBlock;
import org.qbicc.machine.llvm.LLBuilder;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Linkage;
import org.qbicc.machine.llvm.Module;
import org.qbicc.machine.llvm.Types;

import java.util.List;

public class LLVMPseudoIntrinsics {
    private final Module module;

    private final LLValue rawPtrType;
    private final LLValue collectedPtrType;

    private LLValue castPtrToRef;
    private LLValue castPtrToRefType;

    private LLValue castRefToPtr;
    private LLValue castRefToPtrType;

    public LLVMPseudoIntrinsics(Module module) {
        this.module = module;

        rawPtrType = Types.ptrTo(Types.i8);
        collectedPtrType = Types.ptrTo(Types.i8, 1);
    }

    private FunctionDefinition createCastPtrToRef() {
        FunctionDefinition func = module.define("qbicc.internal.cast_ptr_to_ref");
        LLBasicBlock block = func.createBlock();
        LLBuilder builder = LLBuilder.newBuilder(block);

        func.linkage(Linkage.PRIVATE);
        func.returns(collectedPtrType);
        LLValue val = func.param(rawPtrType).name("ptr").asValue();

        builder.ret(
            collectedPtrType,
            builder.addrspacecast(rawPtrType, val, collectedPtrType).asLocal("ref")
        );

        return func;
    }

    private FunctionDefinition createCastRefToPtr() {
        FunctionDefinition func = module.define("qbicc.internal.cast_ref_to_ptr");
        LLBasicBlock block = func.createBlock();
        LLBuilder builder = LLBuilder.newBuilder(block);

        func.linkage(Linkage.PRIVATE);
        func.returns(rawPtrType);
        LLValue val = func.param(collectedPtrType).name("ref").asValue();

        builder.ret(
            rawPtrType,
            builder.addrspacecast(collectedPtrType, val, rawPtrType).asLocal("ptr")
        );

        return func;
    }

    private void ensureCastPtrToRef() {
        if (castPtrToRef == null) {
            castPtrToRef = createCastPtrToRef().asGlobal();
            castPtrToRefType = Types.function(collectedPtrType, List.of(rawPtrType), false);
        }
    }

    private void ensureCastRefToPtr() {
        if (castRefToPtr == null) {
            castRefToPtr = createCastRefToPtr().asGlobal();
            castRefToPtrType = Types.function(rawPtrType, List.of(collectedPtrType), false);
        }
    }

    public LLValue getRawPtrType() {
        return rawPtrType;
    }

    public LLValue getCollectedPtrType() {
        return collectedPtrType;
    }

    public LLValue getCastPtrToRef() {
        ensureCastPtrToRef();
        return castPtrToRef;
    }

    public LLValue getCastPtrToRefType() {
        ensureCastPtrToRef();
        return castPtrToRefType;
    }

    public LLValue getCastRefToPtr() {
        ensureCastRefToPtr();
        return castRefToPtr;
    }

    public LLValue getCastRefToPtrType() {
        ensureCastRefToPtr();
        return castRefToPtrType;
    }
}

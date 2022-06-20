package org.qbicc.plugin.llvm;

import java.util.List;

import org.qbicc.machine.llvm.FunctionAttributes;
import org.qbicc.machine.llvm.FunctionDefinition;
import org.qbicc.machine.llvm.LLBasicBlock;
import org.qbicc.machine.llvm.LLBuilder;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Linkage;
import org.qbicc.machine.llvm.Module;
import org.qbicc.machine.llvm.Types;

public class LLVMPseudoIntrinsics {

    private final Module module;
    private LLVMReferencePointerFactory refFactory;

    private final LLValue rawPtrType;
    private final LLValue collectedPtrType;

    private LLValue castPtrToRef;
    private LLValue castPtrToRefType;

    private LLValue castRefToPtr;
    private LLValue castRefToPtrType;

    public LLVMPseudoIntrinsics(Module module, LLVMReferencePointerFactory refFactory) {
        this.module = module;
        this.refFactory = refFactory;

        this.rawPtrType = refFactory.makeRawPointer();
        this.collectedPtrType = refFactory.makeReferencePointer();
    }

    private FunctionDefinition createCastPtrToRef() {
        FunctionDefinition func = module.define("qbicc.internal.cast_ptr_to_ref");
        LLBasicBlock block = func.createBlock();
        LLBuilder builder = LLBuilder.newBuilder(block);

        func.linkage(Linkage.PRIVATE);
        func.returns(collectedPtrType);
        func.attribute(FunctionAttributes.alwaysinline).attribute(FunctionAttributes.gcLeafFunction);
        LLValue val = func.param(rawPtrType).name("ptr").asValue();

        refFactory.buildCastPtrToRef(builder, val);

        return func;
    }

    private FunctionDefinition createCastRefToPtr() {
        FunctionDefinition func = module.define("qbicc.internal.cast_ref_to_ptr");
        LLBasicBlock block = func.createBlock();
        LLBuilder builder = LLBuilder.newBuilder(block);

        func.linkage(Linkage.PRIVATE);
        func.returns(rawPtrType);
        func.attribute(FunctionAttributes.alwaysinline).attribute(FunctionAttributes.gcLeafFunction);
        LLValue val = func.param(collectedPtrType).name("ref").asValue();

        refFactory.buildCastRefToPtr(builder, val);

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

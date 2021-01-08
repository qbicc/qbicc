package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.AddressNaming;
import cc.quarkus.qcc.machine.llvm.LLBasicBlock;
import cc.quarkus.qcc.machine.llvm.CallingConvention;
import cc.quarkus.qcc.machine.llvm.DllStorageClass;
import cc.quarkus.qcc.machine.llvm.FloatCondition;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.IntCondition;
import cc.quarkus.qcc.machine.llvm.Linkage;
import cc.quarkus.qcc.machine.llvm.RuntimePreemption;
import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.Visibility;
import cc.quarkus.qcc.machine.llvm.op.Alloca;
import cc.quarkus.qcc.machine.llvm.op.Assignment;
import cc.quarkus.qcc.machine.llvm.op.AtomicRmwInstruction;
import cc.quarkus.qcc.machine.llvm.op.Binary;
import cc.quarkus.qcc.machine.llvm.op.Branch;
import cc.quarkus.qcc.machine.llvm.op.Call;
import cc.quarkus.qcc.machine.llvm.op.ExactBinary;
import cc.quarkus.qcc.machine.llvm.op.FastMathBinary;
import cc.quarkus.qcc.machine.llvm.op.FastMathUnary;
import cc.quarkus.qcc.machine.llvm.op.Fence;
import cc.quarkus.qcc.machine.llvm.op.GetElementPtr;
import cc.quarkus.qcc.machine.llvm.op.Load;
import cc.quarkus.qcc.machine.llvm.op.NuwNswBinary;
import cc.quarkus.qcc.machine.llvm.op.OrderingConstraint;
import cc.quarkus.qcc.machine.llvm.op.Phi;
import cc.quarkus.qcc.machine.llvm.op.Return;
import cc.quarkus.qcc.machine.llvm.op.Select;
import cc.quarkus.qcc.machine.llvm.op.Store;
import cc.quarkus.qcc.machine.llvm.op.YieldingInstruction;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class FunctionDefinitionImpl extends AbstractFunction implements FunctionDefinition {
    final ModuleImpl module;
    final BasicBlockImpl rootBlock = new BasicBlockImpl(null, this);
    BasicBlockImpl lastBlock = rootBlock;
    String section;
    RuntimePreemption preemption = RuntimePreemption.PREEMPTABLE;
    private int blockCounter;
    private int localCounter;

    FunctionDefinitionImpl(final ModuleImpl module, final String name) {
        super(name);
        this.module = module;
    }

    public FunctionDefinitionImpl returns(final LLValue returnType) {
        super.returns(returnType);
        return this;
    }

    public FunctionDefinition section(final String section) {
        this.section = Assert.checkNotNullParam("section", section);
        return this;
    }

    public FunctionDefinition preemption(final RuntimePreemption preemption) {
        this.preemption = Assert.checkNotNullParam("preemption", preemption);
        return this;
    }

    ///////////////////
    // Abstract impl //
    ///////////////////

    String keyWord() {
        return "define";
    }

    //////////////////////////
    // Override return type //
    //////////////////////////

    public FunctionDefinitionImpl linkage(final Linkage linkage) {
        super.linkage(linkage);
        return this;
    }

    public FunctionDefinitionImpl visibility(final Visibility visibility) {
        super.visibility(visibility);
        return this;
    }

    public FunctionDefinitionImpl dllStorageClass(final DllStorageClass dllStorageClass) {
        super.dllStorageClass(dllStorageClass);
        return this;
    }

    public FunctionDefinitionImpl callingConvention(final CallingConvention callingConvention) {
        super.callingConvention(callingConvention);
        return this;
    }

    public FunctionDefinitionImpl addressNaming(final AddressNaming addressNaming) {
        super.addressNaming(addressNaming);
        return this;
    }

    public FunctionDefinitionImpl addressSpace(final int addressSpace) {
        super.addressSpace(addressSpace);
        return this;
    }

    public FunctionDefinitionImpl alignment(final int alignment) {
        super.alignment(alignment);
        return this;
    }

    public FunctionDefinitionImpl meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public FunctionDefinitionImpl comment(final String comment) {
        super.comment(comment);
        return this;
    }

    ///////////////////////
    // Basic block stuff //
    ///////////////////////

    public FunctionDefinition functionDefinition() {
        return this;
    }

    public LLBasicBlock createBlock() {
        return lastBlock = new BasicBlockImpl(lastBlock, this);
    }

    public LLBasicBlock name(final String name) {
        throw new UnsupportedOperationException("Cannot set name of root block");
    }

    public Phi phi(final LLValue type) {
        throw new UnsupportedOperationException("Root block cannot have phi nodes");
    }

    public Branch br(final LLBasicBlock dest) {
        return rootBlock.br(dest);
    }

    ////////////
    // Output //
    ////////////

    void appendAfterLinkage(final Appendable target) throws IOException {
        final RuntimePreemption preemption = this.preemption;
        if (preemption != RuntimePreemption.PREEMPTABLE) {
            target.append(preemption.toString());
            target.append(' ');
        }
        super.appendAfterLinkage(target);
    }

    void appendAfterAddressSpace(final Appendable target) throws IOException {
        if (section != null) {
            target.append("section \"");
            target.append(section);
            target.append('"');
            target.append(' ');
        }
        super.appendAfterAddressSpace(target);
    }

    void appendAfterPrologue(final Appendable target) throws IOException {
        appendMeta(target);
        appendAfterPersonality(target);
    }

    void appendAfterPersonality(final Appendable target) throws IOException {
        appendAfterMetadata(target);
    }

    void appendAfterMetadata(final Appendable target) throws IOException {
        target.append(' ');
        target.append('{');
        appendComment(target);
        target.append(System.lineSeparator());
        lastBlock.appendAsBlockTo(target);
        target.append('}');
        target.append(System.lineSeparator());
    }

    ////////////////////////////
    // Basic block delegation //
    ////////////////////////////

    public Assignment assign(final LLValue value) {
        return rootBlock.assign(value);
    }

    public Branch br(final LLValue cond, final LLBasicBlock ifTrue, final LLBasicBlock ifFalse) {
        return rootBlock.br(cond, ifTrue, ifFalse);
    }

    public Return ret() {
        return rootBlock.ret();
    }

    public Return ret(final LLValue type, final LLValue val) {
        return rootBlock.ret(type, val);
    }

    public Select select(final LLValue condType, final LLValue cond, final LLValue valueType, final LLValue trueValue, final LLValue falseValue) {
        return rootBlock.select(condType, cond, valueType, trueValue, falseValue);
    }

    public void unreachable() {
        rootBlock.unreachable();
    }

    public NuwNswBinary add(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.add(type, arg1, arg2);
    }

    public NuwNswBinary sub(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.sub(type, arg1, arg2);
    }

    public NuwNswBinary mul(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.mul(type, arg1, arg2);
    }

    public NuwNswBinary shl(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.shl(type, arg1, arg2);
    }

    public ExactBinary udiv(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.udiv(type, arg1, arg2);
    }

    public ExactBinary sdiv(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.sdiv(type, arg1, arg2);
    }

    public ExactBinary lshr(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.lshr(type, arg1, arg2);
    }

    public ExactBinary ashr(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.ashr(type, arg1, arg2);
    }

    public FastMathBinary fmul(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.fmul(type, arg1, arg2);
    }

    public FastMathBinary fcmp(final FloatCondition cond, final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.fcmp(cond, type, arg1, arg2);
    }

    public FastMathBinary fadd(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.fadd(type, arg1, arg2);
    }

    public FastMathBinary fsub(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.fsub(type, arg1, arg2);
    }

    public FastMathBinary fdiv(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.fdiv(type, arg1, arg2);
    }

    public FastMathBinary frem(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.frem(type, arg1, arg2);
    }

    public FastMathUnary fneg(final LLValue type, final LLValue arg) {
        return rootBlock.fneg(type, arg);
    }

    public Binary icmp(final IntCondition cond, final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.icmp(cond, type, arg1, arg2);
    }

    public Binary and(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.and(type, arg1, arg2);
    }

    public Binary or(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.or(type, arg1, arg2);
    }

    public Binary xor(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.xor(type, arg1, arg2);
    }

    public Binary urem(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.urem(type, arg1, arg2);
    }

    public Binary srem(final LLValue type, final LLValue arg1, final LLValue arg2) {
        return rootBlock.srem(type, arg1, arg2);
    }

    public YieldingInstruction trunc(final LLValue type, final LLValue value, final LLValue toType) {
        return rootBlock.trunc(type, value, toType);
    }

    public YieldingInstruction ftrunc(final LLValue type, final LLValue value, final LLValue toType) {
        return rootBlock.ftrunc(type, value, toType);
    }

    public YieldingInstruction fpext(final LLValue type, final LLValue value, final LLValue toType) {
        return rootBlock.fpext(type, value, toType);
    }

    public YieldingInstruction sext(final LLValue type, final LLValue value, final LLValue toType) {
        return rootBlock.sext(type, value, toType);
    }

    public YieldingInstruction zext(final LLValue type, final LLValue value, final LLValue toType) {
        return rootBlock.zext(type, value, toType);
    }

    public YieldingInstruction bitcast(final LLValue type, final LLValue value, final LLValue toType) {
        return rootBlock.bitcast(type, value, toType);
    }

    public YieldingInstruction fptosi(final LLValue type, final LLValue value, final LLValue toType) {
        return rootBlock.fptosi(type, value, toType);
    }

    public YieldingInstruction fptoui(final LLValue type, final LLValue value, final LLValue toType) {
        return rootBlock.fptoui(type, value, toType);
    }

    public YieldingInstruction sitofp(final LLValue type, final LLValue value, final LLValue toType) {
        return rootBlock.sitofp(type, value, toType);
    }

    public YieldingInstruction uitofp(final LLValue type, final LLValue value, final LLValue toType) {
        return rootBlock.uitofp(type, value, toType);
    }

    public Call call(final LLValue type, final LLValue function) {
        return rootBlock.call(type, function);
    }

    public Call invoke(final LLValue type, final LLValue function, final LLBasicBlock normal, final LLBasicBlock unwind) {
        return rootBlock.invoke(type, function, normal, unwind);
    }

    public Load load(final LLValue type, final LLValue pointeeType, final LLValue pointer) {
        return rootBlock.load(type, pointeeType, pointer);
    }

    public Store store(final LLValue type, final LLValue value, final LLValue pointeeType, final LLValue pointer) {
        return rootBlock.store(type, value, pointeeType, pointer);
    }

    public Fence fence(final OrderingConstraint ordering) {
        return rootBlock.fence(ordering);
    }

    public AtomicRmwInstruction atomicrmw() {
        return rootBlock.atomicrmw();
    }

    public GetElementPtr getelementptr(final LLValue type, final LLValue ptrType, final LLValue pointer) {
        return rootBlock.getelementptr(type, ptrType, pointer);
    }

    public Alloca alloca(final LLValue type) {
        return rootBlock.alloca(type);
    }

    void assignName(final BasicBlockImpl basicBlock) {
        basicBlock.name("B" + Integer.toHexString(blockCounter++));
    }

    int nextLocalId() {
        return localCounter++;
    }
}

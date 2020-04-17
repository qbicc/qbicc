package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.AddressNaming;
import cc.quarkus.qcc.machine.llvm.CallingConvention;
import cc.quarkus.qcc.machine.llvm.DllStorageClass;
import cc.quarkus.qcc.machine.llvm.Linkage;
import cc.quarkus.qcc.machine.llvm.RuntimePreemption;
import cc.quarkus.qcc.machine.llvm.Visibility;
import cc.quarkus.qcc.machine.llvm.BasicBlock;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.op.Assignment;
import cc.quarkus.qcc.machine.llvm.op.AtomicRmwInstruction;
import cc.quarkus.qcc.machine.llvm.op.Binary;
import cc.quarkus.qcc.machine.llvm.op.Branch;
import cc.quarkus.qcc.machine.llvm.op.Call;
import cc.quarkus.qcc.machine.llvm.op.ExactBinary;
import cc.quarkus.qcc.machine.llvm.op.Fence;
import cc.quarkus.qcc.machine.llvm.op.Load;
import cc.quarkus.qcc.machine.llvm.op.NuwNswBinary;
import cc.quarkus.qcc.machine.llvm.op.OrderingConstraint;
import cc.quarkus.qcc.machine.llvm.op.Phi;
import cc.quarkus.qcc.machine.llvm.op.Return;
import cc.quarkus.qcc.machine.llvm.op.Select;
import cc.quarkus.qcc.machine.llvm.op.Store;
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

    public FunctionDefinitionImpl returns(final Value returnType) {
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

    public FunctionDefinitionImpl meta(final String name, final Value data) {
        super.meta(name, data);
        return this;
    }

    ///////////////////////
    // Basic block stuff //
    ///////////////////////

    public FunctionDefinition functionDefinition() {
        return this;
    }

    public BasicBlock createBlock() {
        return lastBlock = new BasicBlockImpl(lastBlock, this);
    }

    public BasicBlock name(final String name) {
        throw new UnsupportedOperationException("Cannot set name of root block");
    }

    public Phi phi(final Value type) {
        throw new UnsupportedOperationException("Root block cannot have phi nodes");
    }

    public Branch br(final BasicBlock dest) {
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
        // todo: personality
        appendAfterPersonality(target);
    }

    void appendAfterPersonality(final Appendable target) throws IOException {
        // todo: metadata
        appendAfterMetadata(target);
    }

    void appendAfterMetadata(final Appendable target) throws IOException {
        target.append(' ');
        target.append('{');
        target.append(System.lineSeparator());
        lastBlock.appendAsBlockTo(target);
        target.append('}');
        target.append(System.lineSeparator());
    }

    ////////////////////////////
    // Basic block delegation //
    ////////////////////////////

    public Assignment assign(final Value value) {
        return rootBlock.assign(value);
    }

    public Branch br(final Value cond, final BasicBlock ifTrue, final BasicBlock ifFalse) {
        return rootBlock.br(cond, ifTrue, ifFalse);
    }

    public Return ret() {
        return rootBlock.ret();
    }

    public Return ret(final Value type, final Value val) {
        return rootBlock.ret(type, val);
    }

    public Select select(final Value condType, final Value cond, final Value valueType, final Value trueValue, final Value falseValue) {
        return rootBlock.select(condType, cond, valueType, trueValue, falseValue);
    }

    public void unreachable() {
        rootBlock.unreachable();
    }

    public NuwNswBinary add(final Value type, final Value arg1, final Value arg2) {
        return rootBlock.add(type, arg1, arg2);
    }

    public NuwNswBinary sub(final Value type, final Value arg1, final Value arg2) {
        return rootBlock.sub(type, arg1, arg2);
    }

    public NuwNswBinary mul(final Value type, final Value arg1, final Value arg2) {
        return rootBlock.mul(type, arg1, arg2);
    }

    public NuwNswBinary shl(final Value type, final Value arg1, final Value arg2) {
        return rootBlock.shl(type, arg1, arg2);
    }

    public ExactBinary udiv(final Value type, final Value arg1, final Value arg2) {
        return rootBlock.udiv(type, arg1, arg2);
    }

    public ExactBinary sdiv(final Value type, final Value arg1, final Value arg2) {
        return rootBlock.sdiv(type, arg1, arg2);
    }

    public ExactBinary lshr(final Value type, final Value arg1, final Value arg2) {
        return rootBlock.lshr(type, arg1, arg2);
    }

    public ExactBinary ashr(final Value type, final Value arg1, final Value arg2) {
        return rootBlock.ashr(type, arg1, arg2);
    }

    public Binary and(final Value type, final Value arg1, final Value arg2) {
        return rootBlock.and(type, arg1, arg2);
    }

    public Binary or(final Value type, final Value arg1, final Value arg2) {
        return rootBlock.or(type, arg1, arg2);
    }

    public Binary xor(final Value type, final Value arg1, final Value arg2) {
        return rootBlock.xor(type, arg1, arg2);
    }

    public Binary urem(final Value type, final Value arg1, final Value arg2) {
        return rootBlock.urem(type, arg1, arg2);
    }

    public Binary srem(final Value type, final Value arg1, final Value arg2) {
        return rootBlock.srem(type, arg1, arg2);
    }

    public Call call(final Value type, final Value function) {
        return rootBlock.call(type, function);
    }

    public Load load(final Value type, final Value pointeeType, final Value pointer) {
        return rootBlock.load(type, pointeeType, pointer);
    }

    public Store store(final Value type, final Value value, final Value pointeeType, final Value pointer) {
        return rootBlock.store(type, value, pointeeType, pointer);
    }

    public Fence fence(final OrderingConstraint ordering) {
        return rootBlock.fence(ordering);
    }

    public AtomicRmwInstruction atomicrmw() {
        return rootBlock.atomicrmw();
    }

    void assignName(final BasicBlockImpl basicBlock) {
        basicBlock.name("B" + Integer.toHexString(blockCounter++));
    }

    int nextLocalId() {
        return localCounter++;
    }
}

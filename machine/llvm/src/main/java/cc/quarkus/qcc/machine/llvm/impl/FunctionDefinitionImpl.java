package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.AddressNaming;
import cc.quarkus.qcc.machine.llvm.CallingConvention;
import cc.quarkus.qcc.machine.llvm.DllStorageClass;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.LLBasicBlock;
import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.Linkage;
import cc.quarkus.qcc.machine.llvm.RuntimePreemption;
import cc.quarkus.qcc.machine.llvm.Visibility;
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

    public FunctionDefinitionImpl variadic() {
        super.variadic();
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

    public LLBasicBlock createBlock() {
        return lastBlock = new BasicBlockImpl(lastBlock, this);
    }

    public LLBasicBlock getRootBlock() {
        return rootBlock;
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
    }

    void assignName(final BasicBlockImpl basicBlock) {
        basicBlock.name("B" + Integer.toHexString(blockCounter++));
    }

    int nextLocalId() {
        return localCounter++;
    }
}

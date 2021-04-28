package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.AddressNaming;
import org.qbicc.machine.llvm.CallingConvention;
import org.qbicc.machine.llvm.DllStorageClass;
import org.qbicc.machine.llvm.FunctionDefinition;
import org.qbicc.machine.llvm.LLBasicBlock;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Linkage;
import org.qbicc.machine.llvm.RuntimePreemption;
import org.qbicc.machine.llvm.Visibility;
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
    private AbstractValue personalityType = null;
    private AbstractValue personalityValue = null;
    private boolean uwtable = false;

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

    public FunctionDefinition unwindTable() {
        this.uwtable = true;
        return this;
    }

    @Override
    public FunctionDefinition personality(final LLValue personalityValue, final LLValue personalityType) {
        this.personalityValue = (AbstractValue) Assert.checkNotNullParam("personalityValue", personalityValue);
        this.personalityType = (AbstractValue) Assert.checkNotNullParam("personalityType", personalityType);
        return this;
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

    private void appendPreemption(final Appendable target) throws IOException {
        if (preemption != RuntimePreemption.PREEMPTABLE) {
            target.append(preemption.toString()).append(' ');
        }
    }

    private void appendUwtable(final Appendable target) throws IOException {
        if (uwtable) {
            target.append(" uwtable");
        }
    }

    private void appendSection(final Appendable target) throws IOException {
        if (section != null) {
            target.append(" section ");
            appendEscapedString(target, section);
        }
    }

    private void appendPersonality(final Appendable target) throws IOException {
        if (personalityValue != null) {
            target.append(" personality ");
            personalityType.appendTo(target);
            personalityValue.appendTo(target);
        }
    }

    @Override
    public Appendable appendTo(Appendable target) throws IOException {
        target.append("define ");
        appendLinkage(target);
        appendPreemption(target);
        appendVisibility(target);
        appendDllStorageClass(target);
        appendCallingConvention(target);
        appendNameAndType(target);
        appendAddressNaming(target);
        appendAddressSpace(target);
        appendUwtable(target);
        appendFunctionAttributes(target);
        appendAlign(target);
        appendSection(target);
        appendPersonality(target);
        appendMeta(target);
        target.append(" {");
        appendComment(target);
        target.append(System.lineSeparator());
        lastBlock.appendAsBlockTo(target);
        target.append('}');

        return target;
    }

    void assignName(final BasicBlockImpl basicBlock) {
        basicBlock.name("B" + Integer.toHexString(blockCounter++));
    }

    int nextLocalId() {
        return localCounter++;
    }
}

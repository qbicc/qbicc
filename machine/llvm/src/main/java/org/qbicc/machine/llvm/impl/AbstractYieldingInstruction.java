package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.YieldingInstruction;

abstract class AbstractYieldingInstruction extends AbstractInstruction implements YieldingInstruction {

    final ModuleImpl module;
    final FunctionDefinitionImpl func;
    AbstractValue lvalue;

    AbstractYieldingInstruction(final ModuleImpl module) {
        this.module = module;
        func = null;
    }

    AbstractYieldingInstruction(final BasicBlockImpl block) {
        this.module = block.func.module;
        func = block.func;
    }

    public LLValue asGlobal() {
        return setLValue(new GlobalValueOf(this, module.nextGlobalId()));
    }

    public LLValue asGlobal(final String name) {
        return setLValue(new NamedGlobalValueOf(name));
    }

    public LLValue asLocal() {
        if (func == null) {
            throw new IllegalStateException("Cannot get global value as local");
        }
        return setLValue(new LocalValueOf(this, func.nextLocalId()));
    }

    public LLValue asLocal(final String name) {
        return setLValue(new NamedLocalValueOf(this, name));
    }

    @Override
    public LLValue setLValue(LLValue value) {
        checkTarget();
        AbstractValue abstractValue = (AbstractValue) value;
        abstractValue.setInstruction(this);
        return lvalue = abstractValue;
    }

    public YieldingInstruction comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public YieldingInstruction meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        final AbstractValue lvalue = this.lvalue;
        if (lvalue != null) {
            lvalue.appendTo(target);
            target.append(" = ");
        }
        return super.appendTo(target);
    }

    private void checkTarget() {
        if (lvalue != null) {
            throw new IllegalStateException("Target already set");
        }
    }
}

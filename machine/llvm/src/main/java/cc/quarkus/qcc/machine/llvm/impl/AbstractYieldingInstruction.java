package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.op.YieldingInstruction;

abstract class AbstractYieldingInstruction extends AbstractInstruction implements YieldingInstruction {

    final BasicBlockImpl block;
    AbstractValue lvalue;

    AbstractYieldingInstruction(final BasicBlockImpl block) {
        this.block = block;
    }

    public Value asGlobal() {
        return setLValue(new GlobalValueOf(this, block.func.module.nextGlobalId()));
    }

    public Value asGlobal(final String name) {
        return setLValue(new NamedGlobalValueOf(name));
    }

    public Value asLocal() {
        return setLValue(new LocalValueOf(this, block.func.nextLocalId()));
    }

    public Value asLocal(final String name) {
        return setLValue(new NamedLocalValueOf(name));
    }

    AbstractValue setLValue(AbstractValue value) {
        checkTarget();
        return lvalue = value;
    }

    public YieldingInstruction meta(final String name, final Value data) {
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

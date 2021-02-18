package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.LLValue;

final class PtrToIntConstant extends AbstractValue {
    final LLValue value;
    final LLValue fromType;
    final LLValue toType;

    PtrToIntConstant(LLValue value, LLValue fromType, LLValue toType) {
        this.value = value;
        this.fromType = fromType;
        this.toType = toType;
    }

    public Appendable appendTo(Appendable target) throws IOException {
        target.append("ptrtoint (");
        ((AbstractValue)fromType).appendTo(target);
        target.append(" ");
        ((AbstractValue)value).appendTo(target);
        target.append(" to ");
        ((AbstractValue)toType).appendTo(target);
        target.append(")");
        return target;
    }
}

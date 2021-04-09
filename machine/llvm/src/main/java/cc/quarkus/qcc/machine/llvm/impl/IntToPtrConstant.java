package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;

final class IntToPtrConstant extends AbstractValue {
    final LLValue value;
    final LLValue fromType;
    final LLValue toType;

    IntToPtrConstant(LLValue value, LLValue fromType, LLValue toType) {
        this.value = value;
        this.fromType = fromType;
        this.toType = toType;
    }

    public Appendable appendTo(Appendable target) throws IOException {
        target.append("inttoptr (");
        ((AbstractValue)fromType).appendTo(target);
        target.append(" ");
        ((AbstractValue)value).appendTo(target);
        target.append(" to ");
        ((AbstractValue)toType).appendTo(target);
        target.append(")");
        return target;
    }
}

package cc.quarkus.qcc.machine.llvm.impl;

import cc.quarkus.qcc.machine.llvm.LLValue;

import java.io.IOException;

public class BitcastConstant extends AbstractValue {
    final LLValue value;
    final LLValue fromType;
    final LLValue toType;

    BitcastConstant(LLValue value, LLValue fromType, LLValue toType) {
        this.value = value;
        this.fromType = fromType;
        this.toType = toType;
    }

    public Appendable appendTo(Appendable target) throws IOException {
        target.append("bitcast (");
        ((AbstractValue)fromType).appendTo(target);
        target.append(" ");
        ((AbstractValue)value).appendTo(target);
        target.append(" to ");
        ((AbstractValue)toType).appendTo(target);
        target.append(")");
        return target;
    }
}

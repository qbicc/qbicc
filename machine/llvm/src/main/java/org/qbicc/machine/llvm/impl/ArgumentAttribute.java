package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;

final class ArgumentAttribute extends AbstractValue {
    final String name;
    final AbstractValue value;

    ArgumentAttribute(String name, LLValue value) {
        this.name = name;
        this.value = (AbstractValue) value;
    }

    public Appendable appendTo(Appendable target) throws IOException {
        target.append(name);
        target.append('(');
        value.appendTo(target);
        target.append(')');
        return target;
    }
}

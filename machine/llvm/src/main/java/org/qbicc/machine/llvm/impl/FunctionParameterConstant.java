package org.qbicc.machine.llvm.impl;

import java.io.IOException;

public class FunctionParameterConstant extends AbstractValue {
    private final String name;
    public FunctionParameterConstant(String name) {
        this.name = name;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('@');
        target.append(name);
        return target;
    }
}

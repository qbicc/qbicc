package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.Set;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.llvm.AsmFlag;

final class AsmExpression extends AbstractValue {

    private final String instruction;
    private final String constraints;
    private final Set<AsmFlag> flags;

    AsmExpression(final String instruction, final String constraints, final Set<AsmFlag> flags) {
        this.instruction = Assert.checkNotNullParam("instruction", instruction);
        this.constraints = Assert.checkNotNullParam("constraints", constraints);
        this.flags = Assert.checkNotNullParam("flags", flags);
    }

    @Override
    public Appendable appendTo(Appendable target) throws IOException {
        target.append("asm").append(' ');
        for (AsmFlag flag : flags) {
            target.append(flag.getLlvmString()).append(' ');
        }
        appendEscapedString(target, instruction);
        target.append(',').append(' ');
        appendEscapedString(target, constraints);
        return target;
    }
}

package org.qbicc.machine.llvm.impl;

import org.qbicc.machine.llvm.LLBasicBlock;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.op.Switch;

import java.io.IOException;

final class SwitchImpl extends AbstractInstruction implements Switch {
    private final AbstractValue type;
    private final AbstractValue value;
    private final BasicBlockImpl defaultTarget;
    private CaseImpl lastCase;

    SwitchImpl(final AbstractValue type, final AbstractValue value, final BasicBlockImpl defaultTarget) {
        super();
        this.type = type;
        this.value = value;
        this.defaultTarget = defaultTarget;
    }

    public Switch meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Switch comment(final String comment) {
        super.comment(comment);
        return this;
    }

    @Override
    public Case case_(LLValue value, LLBasicBlock target) {
        return lastCase = new CaseImpl(this, lastCase, (AbstractValue) value, (BasicBlockImpl) target);
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append("switch ");
        appendValue(target, type);
        target.append(' ');
        value.appendTo(target);
        target.append(", label ");
        defaultTarget.appendTo(target);
        target.append(" [\n");

        if (lastCase != null)
            lastCase.appendTo(target);

        target.append("  ]");
        return appendTrailer(target);
    }

    static final class CaseImpl extends AbstractEmittable implements Case {
        final SwitchImpl switch_;
        final CaseImpl prev;

        final AbstractValue value;
        final BasicBlockImpl target;

        CaseImpl(final SwitchImpl switch_, final CaseImpl prev, final AbstractValue value, final BasicBlockImpl target) {
            this.switch_ = switch_;
            this.prev = prev;
            this.value = value;
            this.target = target;
        }

        public Case case_(LLValue value, LLBasicBlock target) {
            return switch_.case_(value, target);
        }

        public Appendable appendTo(Appendable target) throws IOException {
            if (prev != null)
                prev.appendTo(target);

            target.append("    ");
            appendValue(target, switch_.type);
            target.append(' ');
            value.appendTo(target);
            target.append(", label ");
            this.target.appendTo(target);
            target.append('\n');

            return target;
        }
    }
}

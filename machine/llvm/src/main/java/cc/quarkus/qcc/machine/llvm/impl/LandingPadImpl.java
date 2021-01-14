package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.op.LandingPad;
import io.smallrye.common.constraint.Assert;

final class LandingPadImpl extends AbstractYieldingInstruction implements LandingPad {
    private final AbstractValue resultType;
    boolean cleanup;
    Clause last;

    LandingPadImpl(final BasicBlockImpl block, final AbstractValue resultType) {
        super(block);
        this.resultType = resultType;
    }

    public LandingPad cleanup() {
        cleanup = true;
        return this;
    }

    public LandingPad catch_(final LLValue type, final LLValue value) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        last = new Catch(last, (AbstractValue) type, (AbstractValue) value);
        return this;
    }

    public LandingPad filter(final LLValue type, final LLValue value) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        last = new Filter(last, (AbstractValue) type, (AbstractValue) value);
        return this;
    }

    public LandingPad comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public LandingPad meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append("landingpad");
        target.append(' ');
        resultType.appendTo(target);
        if (cleanup) {
            target.append(' ');
            target.append("cleanup");
        }
        Clause last = this.last;
        if (last != null) {
            last.appendTo(target);
        }
        return target;
    }

    static abstract class Clause extends AbstractEmittable {
        private final Clause prev;
        private final AbstractValue type;
        private final AbstractValue value;

        Clause(final Clause prev, final AbstractValue type, final AbstractValue value) {
            this.prev = prev;
            this.type = type;
            this.value = value;
        }

        abstract String keyword();

        public Appendable appendTo(final Appendable target) throws IOException {
            Clause prev = this.prev;
            if (prev != null) {
                prev.appendTo(target);
            }
            target.append(' ');
            target.append(keyword());
            target.append(' ');
            type.appendTo(target);
            target.append(' ');
            value.appendTo(target);
            return target;
        }
    }

    static final class Catch extends Clause {
        Catch(final Clause prev, final AbstractValue type, final AbstractValue value) {
            super(prev, type, value);
        }

        String keyword() {
            return "catch";
        }
    }

    static final class Filter extends Clause {
        Filter(final Clause prev, final AbstractValue type, final AbstractValue value) {
            super(prev, type, value);
        }

        String keyword() {
            return "filter";
        }
    }

}

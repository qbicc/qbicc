package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.qbicc.machine.llvm.CallingConvention;
import org.qbicc.machine.llvm.FastMathFlag;
import org.qbicc.machine.llvm.TailType;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Types;
import org.qbicc.machine.llvm.op.Call;
import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.llvm.op.HasArguments;

final class CallImpl extends AbstractYieldingInstruction implements Call {
    final AbstractValue type;
    final AbstractValue function;
    final ReturnsImpl returns = new ReturnsImpl();
    final List<AbstractValue> attributes = new ArrayList<>();
    Set<FastMathFlag> flags = Set.of();
    TailType tailType = TailType.notail;
    CallingConvention cconv = CallingConvention.C;
    ArgImpl lastArg;
    BundleImpl lastBundle;
    int addressSpace;

    CallImpl(final BasicBlockImpl basicBlock, final AbstractValue type, final AbstractValue function) {
        super(basicBlock);
        this.type = type;
        this.function = function;
    }

    public Call meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Call attribute(LLValue attribute) {
        attributes.add((AbstractValue) Assert.checkNotNullParam("attribute", attribute));
        return this;
    }

    public HasArguments operandBundle(final String bundleName) {
        return lastBundle = new BundleImpl(lastBundle, bundleName);
    }

    public Call comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public Call withFlags(final Set<FastMathFlag> flags) {
        Assert.checkNotNullParam("flags", flags);
        this.flags = flags;
        return this;
    }

    public Call tail() {
        tailType = TailType.tail;
        return this;
    }

    public Call mustTail() {
        tailType = TailType.musttail;
        return this;
    }

    public Call noTail() {
        tailType = TailType.notail;
        return this;
    }

    public Call cconv(final CallingConvention cconv) {
        Assert.checkNotNullParam("cconv", cconv);
        this.cconv = cconv;
        return this;
    }

    public Returns returns() {
        return returns;
    }

    public Call addrSpace(final int num) {
        Assert.checkMinimumParameter("num", 0, num);
        addressSpace = num;
        return this;
    }

    public Argument arg(final LLValue type, final LLValue value) {
        return lastArg = new ArgImpl(this, lastArg, (AbstractValue) type, (AbstractValue) value);
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        if (notVoidFunctionCall()) {
            super.appendTo(target);
        }
        if (tailType != TailType.notail) {
            target.append(tailType.name()).append(' ');
        }
        target.append("call").append(' ');
        for (FastMathFlag flag : flags) {
            target.append(flag.name()).append(' ');
        }
        if (cconv != CallingConvention.C) {
            target.append(cconv.toString()).append(' ');
        }
        returns.appendTo(target);
        if (addressSpace != 0) {
            target.append("addrspace").append('(').append(Integer.toString(addressSpace)).append(')').append(' ');
        }
        if (! (type instanceof FunctionType ft) || ft.isVariadic()) {
            type.appendTo(target);
        } else {
            ft.returnType.appendTo(target);
        }
        target.append(' ');
        function.appendTo(target);
        target.append('(');
        final ArgImpl lastArg = this.lastArg;
        if (lastArg != null) {
            lastArg.appendTo(target);
        }
        target.append(')');
        for (AbstractValue attribute : attributes) {
            target.append(' ');
            attribute.appendTo(target);
        }
        if (lastBundle != null) {
            target.append(' ');
            target.append('[');
            target.append(' ');
            lastBundle.appendTo(target);
            target.append(' ');
            target.append(']');
        }
        return appendTrailer(target);
    }

    private boolean notVoidFunctionCall() {
        return !(type instanceof FunctionType) || Types.void_ != ((FunctionType) type).returnType;
    }

    static final class ReturnsImpl extends AbstractEmittable implements Returns {
        final List<AbstractValue> attributes = new ArrayList<>();

        public ReturnsImpl attribute(LLValue attribute) {
            attributes.add((AbstractValue) Assert.checkNotNullParam("attribute", attribute));
            return this;
        }

        public Appendable appendTo(Appendable target) throws IOException {
            for (AbstractValue attribute : attributes) {
                attribute.appendTo(target);
                target.append(' ');
            }

            return target;
        }
    }

    static final class ArgImpl extends AbstractEmittable implements Argument {
        final HasArguments hasArguments;
        final ArgImpl prev;
        final AbstractValue type;
        final AbstractValue value;
        final List<AbstractValue> attributes = new ArrayList<>();

        ArgImpl(final HasArguments hasArguments, final ArgImpl prev, final AbstractValue type, final AbstractValue value) {
            this.hasArguments = hasArguments;
            this.prev = prev;
            this.type = type;
            this.value = value;
        }

        public ArgImpl attribute(final LLValue attribute) {
            attributes.add((AbstractValue) Assert.checkNotNullParam("attribute", attribute));
            return this;
        }

        public Argument arg(final LLValue type, final LLValue value) {
            return hasArguments.arg(type, value);
        }

        public Appendable appendTo(final Appendable target) throws IOException {
            final ArgImpl prev = this.prev;
            if (prev != null) {
                prev.appendTo(target);
                target.append(',').append(' ');
            }
            type.appendTo(target);
            target.append(' ');
            for (AbstractValue attribute : attributes) {
                attribute.appendTo(target);
                target.append(' ');
            }
            value.appendTo(target);
            return target;
        }
    }

    static final class BundleImpl extends AbstractEmittable implements HasArguments {
        private final String name;
        private final BundleImpl prev;
        ArgImpl lastArg;

        BundleImpl(BundleImpl prev, String name) {
            this.prev = prev;
            this.name = name;
        }

        @Override
        public Appendable appendTo(Appendable target) throws IOException {
            if (prev != null) {
                prev.appendTo(target);
                target.append(',');
                target.append(' ');
            }
            target.append(LLVM.quoteString(name));
            target.append('(');
            final ArgImpl lastArg = this.lastArg;
            if (lastArg != null) {
                lastArg.appendTo(target);
            }
            target.append(')');
            return null;
        }

        @Override
        public Argument arg(LLValue type, LLValue value) {
            return lastArg = new ArgImpl(this, lastArg, (AbstractValue) type, (AbstractValue) value);
        }
    }
}

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

import static org.qbicc.machine.arch.AddressSpaceConstants.DEFAULT;

final class InvokeImpl extends AbstractYieldingInstruction implements Call {
    final AbstractValue type;
    final AbstractValue function;
    final ReturnsImpl returns = new ReturnsImpl();
    final BasicBlockImpl normal;
    final BasicBlockImpl unwind;
    final List<AbstractValue> attributes = new ArrayList<>();
    Set<FastMathFlag> flags = Set.of();
    TailType tailType = TailType.notail;
    CallingConvention cconv = CallingConvention.C;
    ArgImpl lastArg;
    int addressSpace;

    InvokeImpl(final BasicBlockImpl basicBlock, final AbstractValue type, final AbstractValue function, final BasicBlockImpl normal, final BasicBlockImpl unwind) {
        super(basicBlock);
        this.type = type;
        this.function = function;
        this.normal = normal;
        this.unwind = unwind;
    }

    public Call meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    @Override
    public Call attribute(LLValue attribute) {
        attributes.add((AbstractValue) Assert.checkNotNullParam("attribute", attribute));
        return this;
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
        target.append("invoke").append(' ');
        for (FastMathFlag flag : flags) {
            target.append(flag.name()).append(' ');
        }
        if (cconv != CallingConvention.C) {
            target.append(cconv.toString()).append(' ');
        }
        // todo ret attrs
        if (addressSpace != DEFAULT) {
            target.append("addrspace").append('(').append(Integer.toString(addressSpace)).append(')').append(' ');
        }
        returns.appendTo(target);
        type.appendTo(target).append(' ');
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
        // todo operand bundles
        target.append(' ');
        target.append("to label ");
        normal.appendTo(target);
        target.append(' ');
        target.append("unwind label ");
        unwind.appendTo(target);
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
        final InvokeImpl call;
        final ArgImpl prev;
        final AbstractValue type;
        final AbstractValue value;
        final List<AbstractValue> attributes = new ArrayList<>();

        ArgImpl(final InvokeImpl call, final ArgImpl prev, final AbstractValue type, final AbstractValue value) {
            this.call = call;
            this.prev = prev;
            this.type = type;
            this.value = value;
        }

        public ArgImpl attribute(final LLValue attribute) {
            attributes.add((AbstractValue) Assert.checkNotNullParam("attribute", attribute));
            return this;
        }

        public Argument arg(final LLValue type, final LLValue value) {
            return call.arg(type, value);
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
}

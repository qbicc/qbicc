package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;
import java.util.Set;

import cc.quarkus.qcc.machine.llvm.CallingConvention;
import cc.quarkus.qcc.machine.llvm.FastMathFlag;
import cc.quarkus.qcc.machine.llvm.SignExtension;
import cc.quarkus.qcc.machine.llvm.TailType;
import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.Types;
import cc.quarkus.qcc.machine.llvm.op.Call;
import io.smallrye.common.constraint.Assert;

final class CallImpl extends AbstractYieldingInstruction implements Call {
    final AbstractValue type;
    final AbstractValue function;
    Set<FastMathFlag> flags = Set.of();
    TailType tailType = TailType.notail;
    CallingConvention cconv = CallingConvention.C;
    ArgImpl lastArg;
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
        // todo ret attrs
        if (addressSpace != 0) {
            target.append("addrspace").append('(').append(Integer.toString(addressSpace)).append(')').append(' ');
        }
        type.appendTo(target).append(' ');
        function.appendTo(target);
        target.append('(');
        final ArgImpl lastArg = this.lastArg;
        if (lastArg != null) {
            lastArg.appendTo(target);
        }
        target.append(')');
        // todo func attrs
        // todo operand bundles
        return appendTrailer(target);
    }

    private boolean notVoidFunctionCall() {
        return !(type instanceof FunctionType) || Types.void_ != ((FunctionType) type).returnType;
    }

    static final class ArgImpl extends AbstractEmittable implements Argument {
        final CallImpl call;
        final ArgImpl prev;
        final AbstractValue type;
        final AbstractValue value;
        SignExtension ext = SignExtension.none;
        boolean inReg;

        ArgImpl(final CallImpl call, final ArgImpl prev, final AbstractValue type, final AbstractValue value) {
            this.call = call;
            this.prev = prev;
            this.type = type;
            this.value = value;
        }

        public Argument arg(final LLValue type, final LLValue value) {
            return call.arg(type, value);
        }

        public Argument signExt() {
            ext = SignExtension.signext;
            return this;
        }

        public Argument zeroExt() {
            ext = SignExtension.zeroext;
            return this;
        }

        public Argument inReg() {
            inReg = true;
            return this;
        }

        public Appendable appendTo(final Appendable target) throws IOException {
            final ArgImpl prev = this.prev;
            if (prev != null) {
                prev.appendTo(target);
                target.append(',').append(' ');
            }
            if (inReg) {
                target.append("inreg").append(' ');
            }
            type.appendTo(target);
            target.append(' ');
            if (ext != SignExtension.none) {
                target.append(ext.name()).append(' ');
            }
            value.appendTo(target);
            return target;
        }
    }
}

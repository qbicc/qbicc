package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.debuginfo.DIEncoding;
import org.qbicc.machine.llvm.debuginfo.DIExpression;
import org.qbicc.machine.llvm.debuginfo.DIOpcode;

/**
 *
 */
final class DIExpressionImpl extends AbstractMetadataNode implements DIExpression {

    Arg lastArg;
    final AsValue value = new AsValue();

    DIExpressionImpl(int index) {
        super(index);
    }

    DIExpressionImpl() {
        super(-1);
    }

    @Override
    public DIExpression comment(String comment) {
        super.comment(comment);
        return this;
    }

    @Override
    public Appendable appendTo(Appendable target) throws IOException {
        super.appendTo(target);
        value.appendTo(target);
        return target;
    }

    @Override
    public DIExpression arg(int value) {
        lastArg = new IntArg(lastArg, value);
        return this;
    }

    @Override
    public DIExpression arg(DIOpcode op) {
        lastArg = new EnumArg(lastArg, op);
        return this;
    }

    @Override
    public DIExpression arg(DIEncoding op) {
        lastArg = new EnumArg(lastArg, op);
        return this;
    }

    @Override
    public LLValue asValue() {
        return value;
    }

    final class AsValue extends AbstractValue {
        @Override
        public Appendable appendTo(Appendable target) throws IOException {
            target.append("!DIExpression(");
            if (lastArg != null) {
                lastArg.appendTo(target);
            }
            target.append(")");
            return target;
        }
    }

    static abstract class Arg extends AbstractEmittable {
        final Arg prev;

        Arg(Arg prev) {
            this.prev = prev;
        }

        @Override
        public Appendable appendTo(Appendable target) throws IOException {
            if (prev != null) {
                prev.appendTo(target);
                target.append(',');
                target.append(' ');
            }
            return target;
        }
    }

    static final class IntArg extends Arg {
        final int val;

        IntArg(Arg prev, int val) {
            super(prev);
            this.val = val;
        }

        @Override
        public Appendable appendTo(Appendable target) throws IOException {
            super.appendTo(target);
            appendDecimal(target, val);
            return target;
        }
    }

    static final class EnumArg extends Arg {
        final Enum<?> val;

        EnumArg(Arg prev, Enum<?> val) {
            super(prev);
            this.val = val;
        }

        @Override
        public Appendable appendTo(Appendable target) throws IOException {
            super.appendTo(target);
            target.append(val.toString());
            return target;
        }
    }
}

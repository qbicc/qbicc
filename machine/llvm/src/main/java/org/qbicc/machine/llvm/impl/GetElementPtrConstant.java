package org.qbicc.machine.llvm.impl;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.llvm.LLValue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

final class GetElementPtrConstant extends AbstractValue {
    private final LLValue type;
    private final LLValue ptrType;
    private final LLValue pointer;
    ArgImpl lastArg;

    GetElementPtrConstant(final LLValue type, final LLValue ptrType, final LLValue pointer, LLValue ... args) {
        this.type = type;
        this.ptrType = ptrType;
        this.pointer = pointer;
        Iterator<LLValue> iterator = Arrays.stream(args).iterator();
        while (iterator.hasNext()) {
            LLValue argType = iterator.next();
            LLValue argIndex = iterator.next();
            lastArg = new ArgImpl((AbstractValue)argType, (AbstractValue)argIndex, lastArg);
        }
    }

    public GetElementPtrConstant arg(final boolean inRange, final LLValue type, final LLValue index) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("index", index);
        lastArg = new ArgImpl((AbstractValue) type, (AbstractValue) index, lastArg);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append("getelementptr (");
        target.append(' ');
        ((AbstractValue)type).appendTo(target);
        target.append(',');
        target.append(' ');
        ((AbstractValue)ptrType).appendTo(target);
        target.append(' ');
        ((AbstractValue)pointer).appendTo(target);
        ArgImpl lastArg = this.lastArg;
        if (lastArg != null) {
            lastArg.appendTo(target);
        }
        target.append(")");
        return target;
    }

    private static final class ArgImpl extends AbstractEmittable {
        final boolean inRange;
        final AbstractValue type;
        final AbstractValue index;
        final ArgImpl prev;

        ArgImpl(final AbstractValue type, final AbstractValue index, final ArgImpl prev) {
            this.inRange = false;
            this.type = type;
            this.index = index;
            this.prev = prev;
        }

        public Appendable appendTo(final Appendable target) throws IOException {
            ArgImpl prev = this.prev;
            if (prev != null) {
                prev.appendTo(target);
            }
            target.append(',');
            target.append(' ');
            if (inRange) {
                target.append("inrange");
                target.append(' ');
            }
            type.appendTo(target);
            target.append(' ');
            index.appendTo(target);
            return target;
        }
    }
}
package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.op.GetElementPtr;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class GetElementPtrImpl extends AbstractYieldingInstruction implements GetElementPtr {
    private final AbstractValue type;
    private final AbstractValue ptrType;
    private final AbstractValue pointer;
    ArgImpl lastArg;

    GetElementPtrImpl(final BasicBlockImpl basicBlock, final AbstractValue type, final AbstractValue ptrType, final AbstractValue pointer) {
        super(basicBlock);

        this.type = type;
        this.ptrType = ptrType;
        this.pointer = pointer;
    }

    public GetElementPtr arg(final boolean inRange, final LLValue type, final LLValue index) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("index", index);
        lastArg = new ArgImpl(inRange, (AbstractValue) type, (AbstractValue) index, lastArg);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append("getelementptr");
        target.append(' ');
        type.appendTo(target);
        target.append(',');
        target.append(' ');
        ptrType.appendTo(target);
        target.append(' ');
        pointer.appendTo(target);
        ArgImpl lastArg = this.lastArg;
        if (lastArg != null) {
            lastArg.appendTo(target);
        }
        return target;
    }

    static final class ArgImpl extends AbstractEmittable {
        final boolean inRange;
        final AbstractValue type;
        final AbstractValue index;
        final ArgImpl prev;

        ArgImpl(final boolean inRange, final AbstractValue type, final AbstractValue index, final ArgImpl prev) {
            this.inRange = inRange;
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

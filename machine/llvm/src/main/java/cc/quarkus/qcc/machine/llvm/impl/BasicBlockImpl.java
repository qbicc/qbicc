package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.LLBasicBlock;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class BasicBlockImpl extends AbstractEmittable implements LLBasicBlock {
    final BasicBlockImpl prev;
    final FunctionDefinitionImpl func;
    final List<AbstractEmittable> phis = new ArrayList<>();
    final List<AbstractEmittable> items = new ArrayList<>();
    AbstractEmittable terminator;
    String name;

    BasicBlockImpl(final BasicBlockImpl prev, final FunctionDefinitionImpl func) {
        this.prev = prev;
        this.func = func;
    }

    public LLBasicBlock name(final String name) {
        this.name = Assert.checkNotNullParam("name", name);
        return this;
    }

    public FunctionDefinition functionDefinition() {
        return func;
    }

    @SuppressWarnings("UnusedReturnValue")
    Appendable appendAsBlockTo(final Appendable target) throws IOException {
        final BasicBlockImpl prev = this.prev;
        if (prev != null) {
            prev.appendAsBlockTo(target);
        }
        if (phis.isEmpty() && items.isEmpty() && terminator == null) {
            // no block;
            return target;
        }
        if (terminator == null) {
            throw new IllegalStateException("Basic block not terminated");
        }
        if (this != func.rootBlock) {
            if (name == null) {
                func.assignName(this);
            }
            target.append(name).append(':').append(System.lineSeparator());
        }
        for (List<AbstractEmittable> list : List.of(phis, items)) {
            for (AbstractEmittable item : list) {
                target.append("  ");
                item.appendTo(target);
                target.append(System.lineSeparator());
            }
        }
        target.append("  ");
        terminator.appendTo(target);
        target.append(System.lineSeparator());
        return target;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('%');
        if (this == func.rootBlock) {
            target.append('0');
        } else {
            if (name == null) {
                func.assignName(this);
            }
            target.append(name);
        }
        return target;
    }
}

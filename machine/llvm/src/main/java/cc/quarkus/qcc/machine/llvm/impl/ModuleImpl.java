package cc.quarkus.qcc.machine.llvm.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.machine.llvm.Function;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.Global;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.op.Assignment;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class ModuleImpl implements Module {
    private final List<Emittable> items = new ArrayList<>();
    private int globalCounter;

    private <E extends Emittable> E add(E item) {
        items.add(item);
        return item;
    }

    public FunctionDefinition define(final String name) {
        Assert.checkNotNullParam("name", name);
        return add(new FunctionDefinitionImpl(this, name));
    }

    public Function declare(final String name) {
        Assert.checkNotNullParam("name", name);
        return add(new FunctionDeclarationImpl(name));
    }

    public Assignment assign(final Value type, final Value value) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        return null;
    }

    public Global global(final Value type) {
        Assert.checkNotNullParam("type", type);
        return null;
    }

    public Global constant(final Value type) {
        Assert.checkNotNullParam("type", type);
        return null;
    }

    public Assignment global(final Value type, final Value value) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        return null;
    }

    int nextGlobalId() {
        return globalCounter++;
    }

    public void writeTo(final BufferedWriter output) throws IOException {
        for (Emittable item : items) {
            item.appendTo(output);
            output.newLine();
        }
    }
}

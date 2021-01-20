package cc.quarkus.qcc.machine.llvm.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.machine.llvm.Function;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.Global;
import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.debuginfo.MetadataTuple;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class ModuleImpl implements Module {
    private final List<Emittable> items = new ArrayList<>();
    private int globalCounter;
    private int metadataNodeCounter;

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

    public Global global(final LLValue type) {
        Assert.checkNotNullParam("type", type);
        return add(new GlobalImpl(this, false, (AbstractValue) type));
    }

    public Global constant(final LLValue type) {
        Assert.checkNotNullParam("type", type);
        return add(new GlobalImpl(this, true, (AbstractValue) type));
    }

    public MetadataTuple metadataTuple() {
        return add(new MetadataTupleImpl(nextMetadataNodeId()));
    }

    public MetadataTuple metadataTuple(final String name) {
        Assert.checkNotNullParam("name", name);
        return add(new MetadataTupleImpl(name));
    }

    int nextGlobalId() {
        return globalCounter++;
    }

    int nextMetadataNodeId() {
        return metadataNodeCounter++;
    }

    public void writeTo(final BufferedWriter output) throws IOException {
        for (Emittable item : items) {
            item.appendTo(output);
            output.newLine();
        }
    }
}

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
import cc.quarkus.qcc.machine.llvm.debuginfo.DICompileUnit;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIFile;
import cc.quarkus.qcc.machine.llvm.debuginfo.DILocation;
import cc.quarkus.qcc.machine.llvm.debuginfo.DISubprogram;
import cc.quarkus.qcc.machine.llvm.debuginfo.DISubroutineType;
import cc.quarkus.qcc.machine.llvm.debuginfo.DebugEmissionKind;
import cc.quarkus.qcc.machine.llvm.debuginfo.MetadataTuple;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class ModuleImpl implements Module {
    private final List<Emittable> items = new ArrayList<>();
    private int globalCounter;
    private int metadataNodeCounter;

    private MetadataTuple compileUnits;

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

    public DICompileUnit diCompileUnit(final String language, final LLValue file, final DebugEmissionKind emissionKind) {
        Assert.checkNotNullParam("language", language);
        Assert.checkNotNullParam("file", file);
        Assert.checkNotNullParam("emissionKind", emissionKind);

        DICompileUnitImpl diCompileUnit = new DICompileUnitImpl(nextMetadataNodeId(), language, (AbstractValue)file, emissionKind);

        if (compileUnits == null) {
            compileUnits = metadataTuple("llvm.dbg.cu");
        }
        compileUnits.elem(null, diCompileUnit.asRef());

        return add(diCompileUnit);
    }

    public DIFile diFile(final String filename, final String directory) {
        Assert.checkNotNullParam("filename", filename);
        Assert.checkNotNullParam("directory", directory);
        return add(new DIFileImpl(nextMetadataNodeId(), filename, directory));
    }

    public DILocation diLocation(final int line, final int column, final LLValue scope, final LLValue inlinedAt) {
        Assert.checkNotNullParam("file", scope);
        return add(new DILocationImpl(nextMetadataNodeId(), line, column, (AbstractValue)scope, (AbstractValue)inlinedAt));
    }

    public DISubprogram diSubprogram(final String name, final LLValue type, final LLValue unit) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("unit", unit);
        return add(new DISubprogramImpl(nextMetadataNodeId(), name, (AbstractValue)type, (AbstractValue)unit));
    }

    public DISubroutineType diSubroutineType(final LLValue types) {
        Assert.checkNotNullParam("types", types);
        return add(new DISubroutineTypeImpl(nextMetadataNodeId(), (AbstractValue)types));
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

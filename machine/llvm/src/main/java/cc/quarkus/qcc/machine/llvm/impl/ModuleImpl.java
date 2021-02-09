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
import cc.quarkus.qcc.machine.llvm.IdentifiedType;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIBasicType;
import cc.quarkus.qcc.machine.llvm.debuginfo.DICompileUnit;
import cc.quarkus.qcc.machine.llvm.debuginfo.DICompositeType;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIDerivedType;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIEncoding;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIFile;
import cc.quarkus.qcc.machine.llvm.debuginfo.DILocation;
import cc.quarkus.qcc.machine.llvm.debuginfo.DISubprogram;
import cc.quarkus.qcc.machine.llvm.debuginfo.DISubroutineType;
import cc.quarkus.qcc.machine.llvm.debuginfo.DITag;
import cc.quarkus.qcc.machine.llvm.debuginfo.DebugEmissionKind;
import cc.quarkus.qcc.machine.llvm.debuginfo.MetadataTuple;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class ModuleImpl implements Module {
    private final List<Emittable> types = new ArrayList<>();
    private final List<Emittable> globals = new ArrayList<>();
    private final List<Emittable> functions = new ArrayList<>();
    private final List<Emittable> namedMeta = new ArrayList<>();
    private final List<Emittable> meta = new ArrayList<>();

    private int globalCounter;
    private int metadataNodeCounter;

    private MetadataTuple compileUnits;

    private <E extends Emittable> E add(List<Emittable> itemList, E item) {
        itemList.add(item);
        return item;
    }

    public FunctionDefinition define(final String name) {
        Assert.checkNotNullParam("name", name);
        return add(functions, new FunctionDefinitionImpl(this, name));
    }

    public Function declare(final String name) {
        Assert.checkNotNullParam("name", name);
        return add(functions, new FunctionDeclarationImpl(name));
    }

    public Global global(final LLValue type) {
        Assert.checkNotNullParam("type", type);
        return add(globals, new GlobalImpl(this, false, (AbstractValue) type));
    }

    public Global constant(final LLValue type) {
        Assert.checkNotNullParam("type", type);
        return add(globals, new GlobalImpl(this, true, (AbstractValue) type));
    }

    public IdentifiedType identifiedType() {
        return identifiedType("T" + nextGlobalId());
    }

    public IdentifiedType identifiedType(String name) {
        Assert.checkNotNullParam("name", name);
        return add(types, new IdentifiedTypeImpl(name));
    }

    public MetadataTuple metadataTuple() {
        return add(meta, new MetadataTupleImpl(nextMetadataNodeId()));
    }

    public MetadataTuple metadataTuple(final String name) {
        Assert.checkNotNullParam("name", name);
        return add(namedMeta, new MetadataTupleImpl(name));
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

        return add(meta, diCompileUnit);
    }

    public DIFile diFile(final String filename, final String directory) {
        Assert.checkNotNullParam("filename", filename);
        Assert.checkNotNullParam("directory", directory);
        return add(meta, new DIFileImpl(nextMetadataNodeId(), filename, directory));
    }

    public DILocation diLocation(final int line, final int column, final LLValue scope, final LLValue inlinedAt) {
        Assert.checkNotNullParam("file", scope);
        return add(meta, new DILocationImpl(nextMetadataNodeId(), line, column, (AbstractValue)scope, (AbstractValue)inlinedAt));
    }

    public DISubprogram diSubprogram(final String name, final LLValue type, final LLValue unit) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("unit", unit);
        return add(meta, new DISubprogramImpl(nextMetadataNodeId(), name, (AbstractValue)type, (AbstractValue)unit));
    }

    public DIBasicType diBasicType(final DIEncoding encoding, final long size, final int align) {
        Assert.checkNotNullParam("encoding", encoding);
        return add(meta, new DIBasicTypeImpl(nextMetadataNodeId(), encoding, size, align));
    }

    public DIDerivedType diDerivedType(final DITag tag, final long size, final int align) {
        Assert.checkNotNullParam("tag", tag);
        return add(meta, new DIDerivedTypeImpl(nextMetadataNodeId(), tag, size, align));
    }

    public DICompositeType diCompositeType(final DITag tag, final long size, final int align) {
        Assert.checkNotNullParam("tag", tag);
        return add(meta, new DICompositeTypeImpl(nextMetadataNodeId(), tag, size, align));
    }

    public DISubroutineType diSubroutineType(final LLValue types) {
        Assert.checkNotNullParam("types", types);
        return add(meta, new DISubroutineTypeImpl(nextMetadataNodeId(), (AbstractValue)types));
    }

    int nextGlobalId() {
        return globalCounter++;
    }

    int nextMetadataNodeId() {
        return metadataNodeCounter++;
    }

    private void writeItems(final List<Emittable> items, final BufferedWriter output, boolean lineBetweenItems) throws IOException {
        for (Emittable item : items) {
            item.appendTo(output);
            output.newLine();

            if (lineBetweenItems)
                output.newLine();
        }

        if (!items.isEmpty() && !lineBetweenItems)
            output.newLine();
    }

    public void writeTo(final BufferedWriter output) throws IOException {
        writeItems(types, output, false);
        writeItems(globals, output, false);
        writeItems(functions, output, true);
        writeItems(namedMeta, output, false);
        writeItems(meta, output, false);
    }
}

package org.qbicc.machine.llvm.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.qbicc.machine.llvm.DataLayout;
import org.qbicc.machine.llvm.Function;
import org.qbicc.machine.llvm.FunctionDefinition;
import org.qbicc.machine.llvm.Global;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Module;
import org.qbicc.machine.llvm.IdentifiedType;
import org.qbicc.machine.llvm.ModuleFlagBehavior;
import org.qbicc.machine.llvm.Triple;
import org.qbicc.machine.llvm.Types;
import org.qbicc.machine.llvm.Values;
import org.qbicc.machine.llvm.debuginfo.DIBasicType;
import org.qbicc.machine.llvm.debuginfo.DICompileUnit;
import org.qbicc.machine.llvm.debuginfo.DICompositeType;
import org.qbicc.machine.llvm.debuginfo.DIDerivedType;
import org.qbicc.machine.llvm.debuginfo.DIEncoding;
import org.qbicc.machine.llvm.debuginfo.DIExpression;
import org.qbicc.machine.llvm.debuginfo.DIFile;
import org.qbicc.machine.llvm.debuginfo.DIGlobalVariable;
import org.qbicc.machine.llvm.debuginfo.DIGlobalVariableExpression;
import org.qbicc.machine.llvm.debuginfo.DILocalVariable;
import org.qbicc.machine.llvm.debuginfo.DILocation;
import org.qbicc.machine.llvm.debuginfo.DISubprogram;
import org.qbicc.machine.llvm.debuginfo.DISubrange;
import org.qbicc.machine.llvm.debuginfo.DISubroutineType;
import org.qbicc.machine.llvm.debuginfo.DITag;
import org.qbicc.machine.llvm.debuginfo.DebugEmissionKind;
import org.qbicc.machine.llvm.debuginfo.MetadataTuple;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class ModuleImpl implements Module {
    private final List<Emittable> header = new ArrayList<>();
    private final List<Emittable> types = new ArrayList<>();
    private final List<Emittable> globals = new ArrayList<>();
    private final List<Emittable> functions = new ArrayList<>();
    private final List<Emittable> namedMeta = new ArrayList<>();
    private final List<Emittable> meta = new ArrayList<>();

    private int globalCounter;
    private int metadataNodeCounter;

    private MetadataTuple flags;
    private MetadataTuple compileUnits;

    private <E extends Emittable> E add(List<Emittable> itemList, E item) {
        itemList.add(item);
        return item;
    }

    public DataLayout dataLayout() {
        return add(header, new DataLayoutImpl());
    }

    public Triple triple() {
        return add(header, new TripleImpl());
    }

    public void sourceFileName(final String path) {
        add(header, new SourceFileImpl(path));
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
        Assert.checkNotNullParam("scope", scope);
        return add(meta, new DILocationImpl(nextMetadataNodeId(), line, column, (AbstractValue)scope, (AbstractValue)inlinedAt));
    }

    public DISubprogram diSubprogram(final String name, final LLValue type, final LLValue unit) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("unit", unit);
        return add(meta, new DISubprogramImpl(nextMetadataNodeId(), name, (AbstractValue)type, (AbstractValue)unit));
    }

    public DISubrange diSubrange(final long count) {
        return add(meta, new DISubrangeImpl(nextMetadataNodeId(), count));
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

    public DILocalVariable diLocalVariable(final String name, final LLValue type, final LLValue scope, final LLValue file, final int line, final int align) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("scope", scope);
        Assert.checkNotNullParam("file", file);
        return add(meta, new DILocalVariableImpl(nextMetadataNodeId(), name, (AbstractValue) type, (AbstractValue) scope, (AbstractValue) file, line, align));
    }

    public DIExpression diExpression() {
        return add(meta, new DIExpressionImpl(nextMetadataNodeId()));
    }

    public DIGlobalVariableExpression diGlobalVariableExpression(LLValue var_, LLValue expr) {
        Assert.checkNotNullParam("var_", var_);
        Assert.checkNotNullParam("expr", expr);
        return add(meta, new DIGlobalVariableExpressionImpl(nextMetadataNodeId(), (AbstractValue) var_, (AbstractValue) expr));
    }

    @Override
    public DIGlobalVariable diGlobalVariable(String name, LLValue type, LLValue scope, LLValue file, int line, int align) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("scope", scope);
        Assert.checkNotNullParam("file", file);
        return add(meta, new DIGlobalVariableImpl(nextMetadataNodeId(), name, (AbstractValue) type, (AbstractValue) scope, (AbstractValue) file, line, align));
    }

    int nextGlobalId() {
        return globalCounter++;
    }

    int nextMetadataNodeId() {
        return metadataNodeCounter++;
    }

    public void addFlag(ModuleFlagBehavior behavior, String name, LLValue type, LLValue value) {
        Assert.checkNotNullParam("behavior", behavior);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("value", value);

        if (flags == null) {
            flags = metadataTuple("llvm.module.flags");
        }

        LLValue flag = metadataTuple()
            .elem(Types.i32, Values.intConstant(behavior.value))
            .elem(null, Values.metadataString(name))
            .elem(type, value)
            .asRef();

        flags.elem(null, flag);
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
        writeItems(header, output, true);
        writeItems(types, output, false);
        writeItems(functions, output, true);
        writeItems(globals, output, false);
        writeItems(namedMeta, output, false);
        writeItems(meta, output, false);
    }
}

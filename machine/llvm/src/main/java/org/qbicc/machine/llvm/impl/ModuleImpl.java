package org.qbicc.machine.llvm.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.machine.llvm.Function;
import org.qbicc.machine.llvm.FunctionDefinition;
import org.qbicc.machine.llvm.Global;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Module;
import org.qbicc.machine.llvm.IdentifiedType;
import org.qbicc.machine.llvm.ModuleFlagBehavior;
import org.qbicc.machine.llvm.Types;
import org.qbicc.machine.llvm.Values;
import org.qbicc.machine.llvm.debuginfo.DIBasicType;
import org.qbicc.machine.llvm.debuginfo.DICompileUnit;
import org.qbicc.machine.llvm.debuginfo.DICompositeType;
import org.qbicc.machine.llvm.debuginfo.DIDerivedType;
import org.qbicc.machine.llvm.debuginfo.DIEncoding;
import org.qbicc.machine.llvm.debuginfo.DIFile;
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

    static final class LineColumn {
        final int line;
        final int column;

        LineColumn(int line, int column) {
            this.line = line;
            this.column = column;
        }

        @Override
        public int hashCode() {
            return column * 43987 + line;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof LineColumn && equals((LineColumn) obj);
        }

        boolean equals(LineColumn other) {
            return this == other || other != null && line == other.line && column == other.column;
        }
    }

    private final Map<AbstractValue, Map<AbstractValue, Map<LineColumn, DILocation>>> locationCache = new ConcurrentHashMap<>();
    private final Map<AbstractValue, Map<LineColumn, DILocation>> locationCacheNoInlinedAt = new ConcurrentHashMap<>();

    public DILocation diLocation(final int line, final int column, final LLValue scope, final LLValue inlinedAt) {
        Assert.checkNotNullParam("file", scope);
        AbstractValue scopeVal = (AbstractValue) scope;
        AbstractValue inlinedAtVal = (AbstractValue) inlinedAt;
        Map<AbstractValue, Map<LineColumn, DILocation>> map1 = inlinedAtVal == null ? locationCacheNoInlinedAt : locationCache.computeIfAbsent(inlinedAtVal, ModuleImpl::newMap);
        Map<LineColumn, DILocation> map2 = map1.computeIfAbsent(scopeVal, ModuleImpl::newMap);
        return map2.computeIfAbsent(new LineColumn(line, column), lineColumn -> add(meta, new DILocationImpl(nextMetadataNodeId(), line, column, scopeVal, inlinedAtVal)));
    }

    private static <K, V> Map<K, V> newMap(Object ignored) {
        return new ConcurrentHashMap<>();
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
        writeItems(types, output, false);
        writeItems(globals, output, false);
        writeItems(functions, output, true);
        writeItems(namedMeta, output, false);
        writeItems(meta, output, false);
    }
}

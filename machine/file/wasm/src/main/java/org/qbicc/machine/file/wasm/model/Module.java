package org.qbicc.machine.file.wasm.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.smallrye.common.constraint.Assert;
import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.qbicc.machine.file.bin.BinaryInput;
import org.qbicc.machine.file.bin.BinaryOutput;
import org.qbicc.machine.file.wasm.Data;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.Ops;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.ValType;
import org.qbicc.machine.file.wasm.Wasm;
import org.qbicc.machine.file.wasm.stream.WasmInputStream;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * A WASM module.
 * This API is not thread-safe.
 */
public final class Module implements Named {
    private final String name;
    private final Set<Imported> imported = new LinkedHashSet<>();
    private final Map<String, Export<?>> exportsByName = new LinkedHashMap<>();
    private final Set<Defined> defined = new LinkedHashSet<>();
    private final Map<Memory, List<ActiveSegment>> memoryInitializers = new HashMap<>();
    private final Map<ActiveSegment, Memory> memoriesByInitializer = new HashMap<>();
    private final Map<Table, List<ActiveElement>> tableInitializers = new HashMap<>();
    private final Map<ActiveElement, Table> tablesByInitializer = new HashMap<>();
    private Func startFunc;

    /**
     * Construct a new empty module.
     */
    public Module() {
        this("");
    }

    /**
     * Construct a new instance with a module name.
     *
     * @param name the module name (must not be {@code null})
     */
    public Module(String name) {
        Assert.checkNotNullParam("name", name);
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Get the start function (if any).
     *
     * @return the start function, or {@code null} if no start function is defined
     */
    public Func startFunc() {
        return startFunc;
    }

    /**
     * Set the start function.
     *
     * @param func the start function (must not be {@code null})
     */
    public void startFunc(Func func) {
        Assert.checkNotNullParam("func", func);
        if (func instanceof ImportedFunc ifn) {
            import_(ifn);
        } else if (func instanceof DefinedFunc df) {
            defined.add(df);
        }
        startFunc = func;
    }

    /**
     * Explicitly import an item into the model, even if it is not used.
     *
     * @param import_ the item to import (must not be {@code null})
     * @return the imported item
     * @param <I> the item type
     */
    public <I extends Imported> I import_(I import_) {
        Assert.checkNotNullParam("import_", import_);
        imported.add(import_);
        return import_;
    }

    /**
     * Iterate over each explicitly imported item.
     *
     * @param action the iteration action (must not be {@code null})
     */
    public void forEachImport(Consumer<Imported> action) {
        imported.forEach(action);
    }

    /**
     * Export an item from this module with the given name.
     *
     * @param name the name (must not be {@code null})
     * @param item the item to export (must not be {@code null})
     * @return the export (not {@code null})
     * @param <E> the item type
     */
    @SuppressWarnings("unchecked")
    public <E extends Exportable> Export<E> export(String name, E item) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("item", item);
        Export<E> exp = new Export<>(name, item);
        Export<?> existing = exportsByName.putIfAbsent(name, exp);
        if (existing != null) {
            if (existing.equals(exp)) {
                return (Export<E>) existing;
            } else {
                throw new IllegalArgumentException("Conflicting export");
            }
        }
        if (item instanceof Defined d) {
            defined.add(d);
        }
        return exp;
    }

    public List<ActiveElement> getInitializers(Table table) {
        Assert.checkNotNullParam("table", table);
        return List.copyOf(tableInitializers.getOrDefault(table, List.of()));
    }

    public List<ActiveSegment> getInitializers(Memory memory) {
        Assert.checkNotNullParam("memory", memory);
        return List.copyOf(memoryInitializers.getOrDefault(memory, List.of()));
    }

    public void addInitializer(Table table, ActiveElement element) {
        Assert.checkNotNullParam("table", table);
        Assert.checkNotNullParam("element", element);
        Table existing = tablesByInitializer.putIfAbsent(element, table);
        if (existing != null) {
            throw new IllegalArgumentException("Initializer is already registered to a table");
        }
        tableInitializers.computeIfAbsent(table, Module::newArrayList).add(element);
    }

    public void addInitializer(Memory memory, ActiveSegment segment) {
        Assert.checkNotNullParam("memory", memory);
        Assert.checkNotNullParam("segment", segment);
        Memory existing = memoriesByInitializer.putIfAbsent(segment, memory);
        if (existing != null) {
            throw new IllegalArgumentException("Initializer is already registered to a memory");
        }
        memoryInitializers.computeIfAbsent(memory, Module::newArrayList).add(segment);
    }

    public void forEachExport(Consumer<Export<?>> action) {
        exportsByName.values().forEach(action);
    }

    @SuppressWarnings("unchecked")
    public <E extends Exportable> void forEachExport(Consumer<Export<E>> action, Class<E> kind) {
        Assert.checkNotNullParam("action", action);
        Assert.checkNotNullParam("kind", kind);
        for (Export<?> export : exportsByName.values()) {
            if (kind.isInstance(export.exported())) {
                action.accept((Export<E>) export);
            }
        }
    }

    public Export<?> getExport(String name) {
        return exportsByName.get(name);
    }

    @SuppressWarnings("unchecked")
    public <E extends Exportable> Export<E> getExport(String name, Class<E> kind) {
        Export<?> export = getExport(name);
        kind.cast(export.exported());
        return (Export<E>) export;
    }

    public <D extends Defined> void define(D item) {
        Assert.checkNotNullParam("item", item);
        defined.add(item);
    }

    public void forEachDefinition(Consumer<Defined> consumer) {
        for (Defined item : defined) {
            consumer.accept(item);
        }
    }

    public <D extends Defined> void forEachDefinition(Consumer<? super D> consumer, Class<D> kind) {
        Assert.checkNotNullParam("consumer", consumer);
        Assert.checkNotNullParam("kind", kind);
        for (Defined item : defined) {
            if (kind.isInstance(item)) {
                consumer.accept(kind.cast(item));
            }
        }
    }

    /**
     * Create a minimized copy of this module, with the following characteristics:
     * <ul>
     *     <li>Only items which are reachable as an export or as the start function are included</li>
     *     <li>The order of the retained items is not changed compared to the original module</li>
     * </ul>
     *
     * @return the minimized copy (not {@code null})
     */
    public Module minimized() {
        throw new UnsupportedOperationException("Not yet supported");
    }

    public void saveTo(BinaryOutput out) throws IOException {
        saveTo(new WasmOutputStream(out));
    }

    public void saveTo(WasmOutputStream wos) throws IOException {
        List<FuncType> types = new ArrayList<>();

        Set<DefinedFunc> namedFuncs = new HashSet<>();
        Map<DefinedFunc, Set<Local>> namedLocals = new HashMap<>();

        List<Export<?>> exports = List.copyOf(exportsByName.values());

        List<DefinedFunc> definedFuncs = new ArrayList<>();
        List<DefinedTable> definedTables = new ArrayList<>();
        List<DefinedMemory> definedMemories = new ArrayList<>();
        List<DefinedGlobal> definedGlobals = new ArrayList<>();
        List<DefinedTag> definedTags = new ArrayList<>();

        List<Element> elements = new ArrayList<>();
        List<Segment> segments = new ArrayList<>();

        MutableObjectIntMap<FuncType> typeIdxs = ObjectIntMaps.mutable.empty();
        MutableObjectIntMap<Element> elementIdxs = ObjectIntMaps.mutable.empty();
        MutableObjectIntMap<Func> funcIdxs = ObjectIntMaps.mutable.empty();
        MutableObjectIntMap<Table> tableIdxs = ObjectIntMaps.mutable.empty();
        MutableObjectIntMap<Memory> memoryIdxs = ObjectIntMaps.mutable.empty();
        MutableObjectIntMap<Global> globalIdxs = ObjectIntMaps.mutable.empty();
        MutableObjectIntMap<Segment> segmentIdxs = ObjectIntMaps.mutable.empty();
        MutableObjectIntMap<Tag> tagIdxs = ObjectIntMaps.mutable.empty();

        final Encoder encoder = new Encoder() {
            public int encode(final BranchTarget branchTarget) {
                throw new NoSuchElementException();
            }

            public int encode(final Element element) {
                return elementIdxs.getOrThrow(element);
            }

            public int encode(final Func func) {
                return funcIdxs.getOrThrow(func);
            }

            public int encode(final FuncType type) {
                return typeIdxs.getOrThrow(type);
            }

            public int encode(final Global global) {
                return globalIdxs.getOrThrow(global);
            }

            public int encode(final Local local) {
                throw new NoSuchElementException();
            }

            public int encode(final Memory memory) {
                return memoryIdxs.getOrThrow(memory);
            }

            public int encode(final Table table) {
                return tableIdxs.getOrThrow(table);
            }

            public int encode(final Segment seg) {
                return segmentIdxs.getOrThrow(seg);
            }

            public int encode(final Tag tag) {
                return tagIdxs.getOrThrow(tag);
            }
        };

        // iterate imports
        for (Imported import_ : imported) {
            if (import_ instanceof ImportedFunc func) {
                funcIdxs.put(func, funcIdxs.size());
                register(types, typeIdxs, func.type());
            } else if (import_ instanceof ImportedTable table) {
                tableIdxs.put(table, tableIdxs.size());
                List<ActiveElement> inits = tableInitializers.getOrDefault(table, List.of());
                for (ActiveElement init : inits) {
                    register(elements, elementIdxs, init);
                }
            } else if (import_ instanceof ImportedMemory memory) {
                memoryIdxs.put(memory, memoryIdxs.size());
                List<ActiveSegment> inits = memoryInitializers.getOrDefault(memory, List.of());
                for (ActiveSegment init : inits) {
                    register(segments, segmentIdxs, init);
                }
            } else if (import_ instanceof ImportedGlobal global) {
                globalIdxs.put(global, globalIdxs.size());
            } else if (import_ instanceof ImportedTag tag) {
                tagIdxs.put(tag, tagIdxs.size());
                register(types, typeIdxs, tag.type());
            } else {
                throw new IllegalStateException();
            }
        }

        // iterate definitions
        for (Defined def : defined) {
            if (def instanceof DefinedFunc func) {
                funcIdxs.put(func, funcIdxs.size());
                definedFuncs.add(func);
                register(types, typeIdxs, func.type());
            } else if (def instanceof DefinedTable table) {
                tableIdxs.put(table, tableIdxs.size());
                definedTables.add(table);
                List<ActiveElement> inits = tableInitializers.getOrDefault(table, List.of());
                for (ActiveElement init : inits) {
                    register(elements, elementIdxs, init);
                }
            } else if (def instanceof DefinedMemory memory) {
                memoryIdxs.put(memory, memoryIdxs.size());
                definedMemories.add(memory);
                List<ActiveSegment> inits = memoryInitializers.getOrDefault(memory, List.of());
                for (ActiveSegment init : inits) {
                    register(segments, segmentIdxs, init);
                }
            } else if (def instanceof DefinedGlobal global) {
                globalIdxs.put(global, globalIdxs.size());
                definedGlobals.add(global);
            } else if (def instanceof DefinedTag tag) {
                tagIdxs.put(tag, tagIdxs.size());
                definedTags.add(tag);
                register(types, typeIdxs, tag.type());
            } else {
                throw new IllegalStateException();
            }
        }

        // code analyzer
        Consumer<Insn<?>> insnChecker = new Consumer<Insn<?>>() {
            public void accept(final Insn insn) {
                if (insn instanceof TagInsn i) {
                    register(types, typeIdxs, i.tag().type());
                } else if (insn instanceof TableAndFuncTypeInsn i) {
                    register(types, typeIdxs, i.type());
                } else if (insn instanceof ElementAndTableInsn i) {
                    register(elements, elementIdxs, i.element());
                } else if (insn instanceof ElementInsn i) {
                    register(elements, elementIdxs, i.element());
                } else if (insn instanceof MemoryAndDataInsn i) {
                    register(segments, segmentIdxs, i.segment());
                } else if (insn instanceof DataInsn i) {
                    register(segments, segmentIdxs, i.segment());
                } else if (insn instanceof BlockInsn i) {
                    FuncType type = i.type();
                    // skip registering types which can be specified implicitly (empty or single-value types)
                    if (! type.parameterTypes().isEmpty() || type.resultTypes().size() > 1) {
                        register(types, typeIdxs, type);
                    }
                    i.body().forEach(this);
                }
            }
        };

        // make sure every type, element, and segment reference has an ID
        for (DefinedFunc definedFunc : definedFuncs) {
            definedFunc.body().forEach(insnChecker);
        }
        for (Segment segment : segments) {
            if (segment instanceof ActiveSegment as) {
                as.offset().forEach(insnChecker);
            }
        }
        for (Element element : elements) {
            for (InsnSeq item : element.init()) {
                item.forEach(insnChecker);
            }
            if (element instanceof ActiveElement ae) {
                ae.offset().forEach(insnChecker);
            }
        }

        // header
        wos.rawInt(0x6D736100);
        wos.rawInt(1);

        // write the sections in order

        // types
        if (! types.isEmpty()) {
            wos.rawByte(Wasm.SECTION_TYPE);
            try (WasmOutputStream sos = new WasmOutputStream(BinaryOutput.temporary(wos::byteVec))) {
                sos.u32(types.size());
                for (FuncType type : types) {
                    sos.funcType(type);
                }
            }
        }
        // imports
        if (! imported.isEmpty()) {
            wos.rawByte(Wasm.SECTION_IMPORT);
            try (WasmOutputStream sos = new WasmOutputStream(BinaryOutput.temporary(wos::byteVec))) {
                sos.u32(imported.size());
                for (Imported imported : imported) {
                    sos.utf8(imported.moduleName());
                    sos.utf8(imported.name());
                    if (imported instanceof ImportedFunc f) {
                        sos.rawByte(0x00);
                        sos.u32(typeIdxs.getOrThrow(f.type()));
                    } else if (imported instanceof ImportedTable t) {
                        sos.rawByte(0x01);
                        sos.type(t.type());
                        writeLimits(sos, t);
                    } else if (imported instanceof ImportedMemory m) {
                        sos.rawByte(0x02);
                        writeLimits(sos, m);
                    } else if (imported instanceof ImportedGlobal g) {
                        sos.rawByte(0x03);
                        sos.type(g.type());
                        sos.mut(g.mutability());
                    } else if (imported instanceof ImportedTag t) {
                        sos.rawByte(0x04);
                        sos.rawByte(0x00); // attribute always 0 for exception for now
                        sos.u32(typeIdxs.getOrThrow(t.type()));
                    } else {
                        throw new IllegalStateException();
                    }
                }
            }
        }
        // functions
        if (! definedFuncs.isEmpty()) {
            wos.rawByte(Wasm.SECTION_FUNCTION);
            try (WasmOutputStream sos = new WasmOutputStream(BinaryOutput.temporary(wos::byteVec))) {
                sos.u32(definedFuncs.size());
                for (DefinedFunc func : definedFuncs) {
                    sos.u32(typeIdxs.getOrThrow(func.type()));
                    if (! func.isAnonymous()) {
                        namedFuncs.add(func);
                    }
                }
            }
        }
        // tables
        if (! definedTables.isEmpty()) {
            wos.rawByte(Wasm.SECTION_TABLE);
            try (WasmOutputStream sos = new WasmOutputStream(BinaryOutput.temporary(wos::byteVec))) {
                sos.u32(definedTables.size());
                for (DefinedTable table : definedTables) {
                    sos.type(table.type());
                    writeLimits(sos, table);
                }
            }
        }
        // memories
        if (! definedMemories.isEmpty()) {
            wos.rawByte(Wasm.SECTION_MEMORY);
            try (WasmOutputStream sos = new WasmOutputStream(BinaryOutput.temporary(wos::byteVec))) {
                sos.u32(definedMemories.size());
                for (DefinedMemory memory : definedMemories) {
                    writeLimits(sos, memory);
                }
            }
        }
        // tag
        if (! definedTags.isEmpty()) {
            wos.rawByte(Wasm.SECTION_TAG);
            try (WasmOutputStream sos = new WasmOutputStream(BinaryOutput.temporary(wos::byteVec))) {
                sos.u32(definedTags.size());
                for (DefinedTag tag : definedTags) {
                    sos.funcType(tag.type());
                }
            }
        }
        // globals
        if (! definedGlobals.isEmpty()) {
            wos.rawByte(Wasm.SECTION_GLOBAL);
            try (WasmOutputStream sos = new WasmOutputStream(BinaryOutput.temporary(wos::byteVec))) {
                sos.u32(definedGlobals.size());
                for (DefinedGlobal global : definedGlobals) {
                    sos.type(global.type());
                    sos.mut(global.mutability());
                    global.init().writeTo(sos, encoder);
                }
            }
        }
        // exports
        if (! exports.isEmpty()) {
            wos.rawByte(Wasm.SECTION_EXPORT);
            try (WasmOutputStream sos = new WasmOutputStream(BinaryOutput.temporary(wos::byteVec))) {
                sos.u32(exports.size());
                for (Export<?> export : exports) {
                    sos.utf8(export.name());
                    Exportable exported = export.exported();
                    if (exported instanceof Func f) {
                        sos.rawByte(0x00);
                        sos.u32(funcIdxs.getOrThrow(f));
                    } else if (exported instanceof Table t) {
                        sos.rawByte(0x01);
                        sos.u32(tableIdxs.getOrThrow(t));
                    } else if (exported instanceof Memory m) {
                        sos.rawByte(0x02);
                        sos.u32(memoryIdxs.getOrThrow(m));
                    } else if (exported instanceof Global g) {
                        sos.rawByte(0x03);
                        sos.u32(globalIdxs.getOrThrow(g));
                    } else if (exported instanceof Tag t) {
                        sos.rawByte(0x04);
                        sos.u32(tagIdxs.getOrThrow(t));
                    }
                }
            }
        }
        // start function
        if (startFunc != null) {
            wos.rawByte(Wasm.SECTION_START);
            int id = funcIdxs.getOrThrow(startFunc);
            wos.u32(Wasm.uleb_size(id));
            wos.u32(id);
        }
        // element section
        if (! elements.isEmpty()) {
            wos.rawByte(Wasm.SECTION_ELEMENT);
            try (WasmOutputStream sos = new WasmOutputStream(BinaryOutput.temporary(wos::byteVec))) {
                sos.u32(elements.size());
                for (Element element : elements) {
                    RefType type = element.type();
                    boolean exprInit = type != RefType.funcref || ! element.isFuncList();
                    int mode = exprInit ? 0b100 : 0b000;
                    boolean active = element instanceof ActiveElement;
                    if (active) {
                        ActiveElement ae = (ActiveElement) element;
                        Table table = tablesByInitializer.get(ae);
                        int tableIdx = tableIdxs.getOrThrow(table);
                        if (tableIdx != 0) {
                            mode |= 0b010;
                            // use second form
                            sos.u32(mode);
                            sos.u32(tableIdx);
                            // offset
                            ae.offset().writeTo(sos, encoder);
                        } else {
                            // use first form
                            sos.u32(mode);
                            // offset
                            ae.offset().writeTo(sos, encoder);
                        }
                    } else {
                        mode |= 0b001;
                        if (element instanceof DeclarativeElement) mode |= 0b010;
                        sos.u32(mode);
                    }
                    if (0 < mode && mode <= 3) {
                        sos.rawByte(0x00); // elemKind == 0 (funcref)
                    } else if (4 < mode && mode <= 7) {
                        sos.type(type);
                    } // else skip
                    List<InsnSeq> items = element.init();
                    if (exprInit) {
                        sos.u32(items.size());
                        for (InsnSeq item : items) {
                            item.writeTo(sos, encoder);
                        }
                    } else {
                        // serialize it as a func list instead
                        assert type == RefType.funcref;
                        int cnt = items.size();
                        sos.u32(cnt);
                        for (int i = 0; i < cnt; i++) {
                            InsnSeq item = items.get(i);
                            FuncInsn insn = (FuncInsn) item.iterator().next();
                            sos.u32(encoder.encode(insn.func()));
                        }
                    }
                }
            }
        }
        if (! segments.isEmpty()) {
            wos.rawByte(Wasm.SECTION_DATA_COUNT);
            int size = segments.size();
            wos.u32(Wasm.uleb_size(size));
            wos.u32(size);
        }
        Map<DefinedFunc, ObjectIntMap<Local>> localsByFunc;
        if (! definedFuncs.isEmpty()) {
            localsByFunc = new HashMap<>(definedFuncs.size());
            wos.rawByte(Wasm.SECTION_CODE);
            try (WasmOutputStream sos = new WasmOutputStream(BinaryOutput.temporary(wos::byteVec))) {
                sos.u32(definedFuncs.size());
                for (DefinedFunc definedFunc : definedFuncs) {
                    try (WasmOutputStream fos = new WasmOutputStream(BinaryOutput.temporary(sos::byteVec))) {
                        List<Local> locals = definedFunc.locals();
                        // create an index by type
                        Map<ValType, List<Local>> localsByType = new HashMap<>();
                        for (Local local : locals) {
                            localsByType.computeIfAbsent(local.type(), Module::newArrayList).add(local);
                            if (! local.isAnonymous()) {
                                namedLocals.computeIfAbsent(definedFunc, Module::newSet).add(local);
                            }
                        }
                        MutableObjectIntMap<Local> localIdxs = ObjectIntMaps.mutable.empty();
                        // parameter indexes first
                        for (Local parameter : definedFunc.parameters()) {
                            localIdxs.put(parameter, localIdxs.size());
                            if (! parameter.isAnonymous()) {
                                namedLocals.computeIfAbsent(definedFunc, Module::newSet).add(parameter);
                            }
                        }
                        fos.u32(localsByType.size());
                        for (Map.Entry<ValType, List<Local>> entry : localsByType.entrySet()) {
                            ValType type = entry.getKey();
                            List<Local> list = entry.getValue();
                            for (Local local : list) {
                                localIdxs.put(local, localIdxs.size());
                            }
                            fos.u32(list.size());
                            fos.type(type);
                        }
                        localsByFunc.put(definedFunc, localIdxs);
                        // now emit the code
                        definedFunc.body().writeTo(fos, new Encoder.Delegating() {
                            @Override
                            public Encoder delegate() {
                                return encoder;
                            }

                            @Override
                            public int encode(Local local) {
                                return localIdxs.getOrThrow(local);
                            }

                            @Override
                            public int encode(BranchTarget branchTarget) {
                                return branchTarget == definedFunc ? 0 : 1 + delegate().encode(branchTarget);
                            }
                        });
                    }
                }
            }
        } else {
            localsByFunc = Map.of();
        }
        if (! segments.isEmpty()) {
            wos.rawByte(Wasm.SECTION_DATA);
            try (WasmOutputStream sos = new WasmOutputStream(BinaryOutput.temporary(wos::byteVec))) {
                sos.u32(segments.size());
                for (Segment segment : segments) {
                    if (segment instanceof ActiveSegment as) {
                        Memory memory = memoriesByInitializer.get(as);
                        int memIdx = memoryIdxs.getOrThrow(memory);
                        if (memIdx != 0) {
                            sos.u32(2);
                            sos.u32(memIdx);
                        } else {
                            sos.u32(0);
                        }
                        as.offset().writeTo(sos, encoder);
                    } else {
                        sos.u32(1);
                    }
                    try (BinaryOutput tbo = BinaryOutput.temporary(sos::byteVec)) {
                        segment.data().writeTo(tbo.asOutputStream());
                    }
                }
            }
        }
        // names
        wos.rawByte(Wasm.SECTION_CUSTOM);
        try (WasmOutputStream sos = new WasmOutputStream(BinaryOutput.temporary(wos::byteVec))) {
            sos.utf8("name");
            // module name
            if (! isAnonymous()) {
                sos.rawByte(0);
                sos.utf8(name());
            }
            // function names
            if (! namedFuncs.isEmpty()) {
                sos.rawByte(1);
                sos.u32(namedFuncs.size());
                for (DefinedFunc func : namedFuncs) {
                    sos.u32(funcIdxs.getOrThrow(func));
                    sos.utf8(func.name());
                }
            }
            // local names
            if (! namedLocals.isEmpty()) {
                sos.rawByte(2);
                sos.u32(namedLocals.size());
                for (Map.Entry<DefinedFunc, Set<Local>> entry : namedLocals.entrySet()) {
                    DefinedFunc func = entry.getKey();
                    sos.u32(funcIdxs.getOrThrow(func));
                    Set<Local> locals = entry.getValue();
                    sos.u32(locals.size());
                    for (Local local : locals) {
                        sos.u32(localsByFunc.get(func).get(local));
                        sos.utf8(local.name());
                    }
                }
            }
        }
    }

    public static Module loadFrom(final BinaryInput in) throws IOException {
        return loadFrom(new WasmInputStream(in));
    }

    public static Module loadFrom(WasmInputStream wis) throws IOException {
        int magic = wis.rawInt();
        if (magic != 0x6D736100) {
            throw new IOException("Invalid magic");
        }
        int version = wis.rawInt();
        if (version != 1) {
            throw new IOException("Unknown version " + version);
        }

        int sectId = wis.optByte();
        // some sections we can read right away, some we have to defer
        if (sectId == -1) {
            // an empty module
            return new Module();
        }
        // instruction sequences for shared cache
        Supplier<InsnSeq> insnSeqFactory;
        Supplier<InsnSeq> constInsnSeqFactory;
        {
            InsnSeq basis = new InsnSeq();
            insnSeqFactory = basis::newWithSharedCache;
            constInsnSeqFactory = () -> basis.newWithSharedCache(InsnSeq.Flag.CONSTANT);
        }
        EnumMap<Wasm.Section, WasmInputStream> sectionStreams = new EnumMap<>(Wasm.Section.class);
        try {
            Wasm.Section lastSection = null;
            Wasm.Section section;
            while (sectId != -1) {
                int size = wis.u32();
                section = Wasm.Section.forId(sectId);
                WasmInputStream stream = wis.fork(size);
                if (section == Wasm.Section.CUSTOM) {
                    try {
                        section = Wasm.Section.forName(stream.utf8());
                    } catch (Throwable t) {
                        try {
                            stream.close();
                        } catch (Throwable t2) {
                            t.addSuppressed(t2);
                        }
                    }
                }
                if (section != null) {
                    if (sectionStreams.putIfAbsent(section, stream) != null) {
                        throw new IOException("Duplicate section " + section + " encountered");
                    }
                    if (lastSection != null && section.precedes(lastSection)) {
                        throw new IOException("Sections in wrong order (" + lastSection + " before " + section + ")");
                    }
                    lastSection = section;
                }
                wis.skip(size);
                // next section!
                sectId = wis.optByte();
            }

            // now, process the sections in a useful order

            // Names
            String moduleName = "";
            IntObjectMap<String> funcNames = IntObjectMaps.immutable.empty();
            IntObjectMap<IntObjectMap<String>> localNames = IntObjectMaps.immutable.empty();
            IntObjectMap<String> globalNames = IntObjectMaps.immutable.empty();
            IntObjectMap<String> elementNames = IntObjectMaps.immutable.empty();
            IntObjectMap<String> segmentNames = IntObjectMaps.immutable.empty();
            if (sectionStreams.containsKey(Wasm.Section.NAME)) {
                try (WasmInputStream sis = sectionStreams.remove(Wasm.Section.NAME)) {
                    int subSec;
                    while ((subSec = sis.optByte()) != -1) {
                        switch (subSec) {
                            case 0 -> moduleName = sis.utf8();
                            case 1 -> {
                                int cnt = sis.u32();
                                MutableIntObjectMap<String> map = IntObjectMaps.mutable.ofInitialCapacity(cnt);
                                for (int i = 0; i < cnt; i ++) {
                                    map.put(sis.u32(), sis.utf8());
                                }
                                funcNames = map;
                            }
                            case 2 -> {
                                int cnt = sis.u32();
                                MutableIntObjectMap<IntObjectMap<String>> map = IntObjectMaps.mutable.ofInitialCapacity(cnt);
                                for (int i = 0; i < cnt; i ++) {
                                    MutableIntObjectMap<String> subMap = IntObjectMaps.mutable.ofInitialCapacity(cnt);
                                    int funcId = sis.u32();
                                    int subCnt = sis.u32();
                                    for (int j = 0; j < subCnt; j ++) {
                                        subMap.put(sis.u32(), sis.utf8());
                                    }
                                    map.put(funcId, subMap);
                                }
                                localNames = map;
                            }
                        }
                    }
                }
            }

            // Types
            List<FuncType> types;
            if (sectionStreams.containsKey(Wasm.Section.TYPE)) {
                try (WasmInputStream sis = sectionStreams.remove(Wasm.Section.TYPE)) {
                    int cnt = sis.u32();
                    types = new ArrayList<>(cnt);
                    for (int i = 0; i < cnt; i ++) {
                        types.add(sis.funcType());
                    }
                }
            } else {
                types = List.of();
            }

            // Imports
            List<Imported> imports;
            ArrayList<Func> funcs = new ArrayList<>(0);
            ArrayList<Table> tables = new ArrayList<>(0);
            ArrayList<Memory> memories = new ArrayList<>(0);
            ArrayList<Global> globals = new ArrayList<>(0);
            ArrayList<Tag> tags = new ArrayList<>(0);
            if (sectionStreams.containsKey(Wasm.Section.IMPORT)) {
                try (WasmInputStream sis = sectionStreams.remove(Wasm.Section.IMPORT)) {
                    int cnt = sis.u32();
                    imports = new ArrayList<>(cnt);
                    // be safe...
                    funcs.ensureCapacity(cnt);
                    tables.ensureCapacity(cnt);
                    memories.ensureCapacity(cnt);
                    globals.ensureCapacity(cnt);
                    tags.ensureCapacity(cnt);
                    for (int i = 0; i < cnt; i ++) {
                        String impModName = sis.utf8();
                        String impName = sis.utf8();
                        switch (sis.rawByte()) {
                            case 0x00 -> {
                                ImportedFunc func = new ImportedFunc(impModName, impName, types.get(sis.u32()));
                                funcs.add(func);
                                imports.add(func);
                            }
                            case 0x01 -> {
                                RefType type = sis.refType();
                                ImportedTable table;
                                switch (sis.rawByte()) {
                                    case 0x00 -> table = new ImportedTable(impModName, impName, type, sis.u64());
                                    case 0x01 -> table = new ImportedTable(impModName, impName, type, sis.u64(), sis.u64());
                                    case 0x03 -> table = new ImportedTable(impModName, impName, type, sis.u64(), sis.u64(), true);
                                    default -> throw new IOException("Invalid table limits");
                                }
                                tables.add(table);
                                imports.add(table);
                            }
                            case 0x02 -> {
                                ImportedMemory memory;
                                switch (sis.rawByte()) {
                                    case 0x00 -> memory = new ImportedMemory(impModName, impName, sis.u64());
                                    case 0x01 -> memory = new ImportedMemory(impModName, impName, sis.u64(), sis.u64());
                                    case 0x03 -> memory = new ImportedMemory(impModName, impName, sis.u64(), sis.u64(), true);
                                    default -> throw new IOException("Invalid memory limits");
                                }
                                memories.add(memory);
                                imports.add(memory);
                            }
                            case 0x03 -> {
                                ImportedGlobal global = new ImportedGlobal(impModName, impName, sis.type(), sis.mut());
                                globals.add(global);
                                imports.add(global);
                            }
                        }
                    }
                }
            } else {
                imports = List.of();
            }

            // Functions
            List<FuncType> definedFuncTypes;
            if (sectionStreams.containsKey(Wasm.Section.FUNCTION)) {
                try (WasmInputStream sis = sectionStreams.remove(Wasm.Section.FUNCTION)) {
                    int cnt = sis.u32();
                    definedFuncTypes = new ArrayList<>(cnt);
                    funcs.ensureCapacity(funcs.size() + cnt);
                    for (int i = 0; i < cnt; i ++) {
                        definedFuncTypes.add(types.get(sis.u32()));
                    }
                }
            } else {
                definedFuncTypes = List.of();
            }

            ArrayList<Defined> defined = new ArrayList<>(0);

            // Tables
            if (sectionStreams.containsKey(Wasm.Section.TABLE)) {
                try (WasmInputStream sis = sectionStreams.remove(Wasm.Section.TABLE)) {
                    int cnt = sis.u32();
                    tables.ensureCapacity(tables.size() + cnt);
                    defined.ensureCapacity(cnt);
                    for (int i = 0; i < cnt; i ++) {
                        RefType type = sis.refType();
                        DefinedTable table = switch (sis.rawByte()) {
                            case 0x00 -> new DefinedTable(type, sis.u64());
                            case 0x01 -> new DefinedTable(type, sis.u64(), sis.u64());
                            case 0x03 -> new DefinedTable(type, sis.u64(), sis.u64(), true);
                            default -> throw new IOException("Invalid table limits");
                        };
                        tables.add(table);
                        defined.add(table);
                    }
                }
            }

            // Memories
            if (sectionStreams.containsKey(Wasm.Section.MEMORY)) {
                try (WasmInputStream sis = sectionStreams.remove(Wasm.Section.MEMORY)) {
                    int cnt = sis.u32();
                    memories.ensureCapacity(memories.size() + cnt);
                    defined.ensureCapacity(defined.size() + cnt);
                    for (int i = 0; i < cnt; i++) {
                        DefinedMemory memory = switch (sis.rawByte()) {
                            case 0x00 -> new DefinedMemory(sis.u64());
                            case 0x01 -> new DefinedMemory(sis.u64(), sis.u64());
                            case 0x03 -> new DefinedMemory(sis.u64(), sis.u64(), true);
                            default -> throw new IOException("Invalid memory limits");
                        };
                        memories.add(memory);
                        defined.add(memory);
                    }
                }
            }

            MutableObjectIntMap<InsnSeq> funcListInitializers = ObjectIntMaps.mutable.empty();
            Map<InsnSeq, WasmInputStream> initializers = new HashMap<>();
            Map<DefinedFunc, WasmInputStream> funcBodies = new HashMap<>();
            // after this point, we must ensure that all initializers are cleaned up
            try {
                // Globals
                if (sectionStreams.containsKey(Wasm.Section.GLOBAL)) {
                    try (WasmInputStream sis = sectionStreams.remove(Wasm.Section.GLOBAL)) {
                        int base = globals.size();
                        int cnt = sis.u32();
                        globals.ensureCapacity(base + cnt);
                        for (int i = 0; i < cnt; i++) {
                            String name = Objects.requireNonNullElse(globalNames.get(base + i), "");
                            InsnSeq seq = constInsnSeqFactory.get();
                            DefinedGlobal global = new DefinedGlobal(name, sis.type(), sis.mut(), seq);
                            initializers.put(seq, sis.fork());
                            InsnSeq.skip(sis);
                            globals.add(global);
                            defined.add(global);
                        }
                    }
                }

                // Elements
                List<Element> elements;
                Map<ActiveElement, Table> tableInitializers;
                if (sectionStreams.containsKey(Wasm.Section.ELEMENT)) {
                    try (WasmInputStream sis = sectionStreams.remove(Wasm.Section.ELEMENT)) {
                        int cnt = sis.u32();
                        elements = new ArrayList<>(cnt);
                        // this is conservative sizing
                        tableInitializers = new HashMap<>(cnt);
                        for (int i = 0; i < cnt; i ++) {
                            int flags = sis.u32();
                            boolean passive = (flags & 0b001) != 0;
                            boolean declarative = passive && (flags & 0b010) != 0;
                            boolean active = ! passive;
                            boolean hasTableIdx = active && (flags & 0b010) != 0;
                            boolean alwaysFuncRef = active && ! hasTableIdx;
                            // also implies reftype instead of elemkind
                            boolean exprInit = (flags & 0b100) != 0;
                            boolean hasElemKind = ! exprInit & ! alwaysFuncRef;
                            boolean hasRefType = exprInit && ! alwaysFuncRef;
                            int tableIdx = 0;
                            InsnSeq offset = null;
                            if (active) {
                                if (hasTableIdx) {
                                    tableIdx = sis.u32();
                                }
                                offset = constInsnSeqFactory.get();
                                initializers.put(offset, sis.fork());
                                InsnSeq.skip(sis);
                            }
                            // common code
                            RefType type;
                            if (alwaysFuncRef) {
                                type = RefType.funcref;
                            } else if (hasElemKind) {
                                type = switch (sis.u32()) {
                                    case 0x00 -> RefType.funcref;
                                    default -> throw new IOException("Invalid element kind");
                                };
                            } else {
                                assert hasRefType;
                                type = sis.refType();
                            }
                            int initCnt = sis.u32();
                            InsnSeq[] inits = new InsnSeq[initCnt];
                            if (exprInit) {
                                for (int j = 0; j < initCnt; j++) {
                                    inits[j] = constInsnSeqFactory.get();
                                    initializers.put(inits[j], sis.fork());
                                    InsnSeq.skip(sis);
                                }
                            } else {
                                for (int j = 0; j < initCnt; j++) {
                                    inits[j] = constInsnSeqFactory.get();
                                    funcListInitializers.put(inits[j], sis.u32());
                                }
                            }
                            String elementName = Objects.requireNonNullElse(elementNames.get(i), "");
                            // create the element
                            Element element;
                            if (declarative) {
                                element = new DeclarativeElement(elementName, type, List.of(inits));
                            } else if (passive) {
                                element = new PassiveElement(elementName, type, List.of(inits));
                            } else {
                                assert active;
                                ActiveElement ae = new ActiveElement(elementName, offset, type, List.of(inits));
                                tableInitializers.put(ae, tables.get(tableIdx));
                                element = ae;
                            }
                            elements.add(element);
                        }
                    }
                } else {
                    elements = List.of();
                    tableInitializers = Map.of();
                }

                // Code section
                if (sectionStreams.containsKey(Wasm.Section.CODE)) {
                    try (WasmInputStream sis = sectionStreams.remove(Wasm.Section.CODE)) {
                        int cnt = sis.u32();
                        if (cnt != definedFuncTypes.size()) {
                            throw new IOException("Inconsistent function count");
                        }
                        defined.ensureCapacity(defined.size() + cnt);
                        for (int i = 0; i < cnt; i++) {
                            int size = sis.u32();
                            WasmInputStream cis = sis.fork(size);
                            try {
                                sis.skip(size);
                                // use `cis` from here on; consume locals and leave the cursor pointed at the instructions
                                FuncType funcType = definedFuncTypes.get(i);
                                List<ValType> parameterTypes = funcType.parameterTypes();
                                List<Local> parameters = parameterTypes.isEmpty() ? List.of() : new ArrayList<>(parameterTypes.size());
                                int li = 0;
                                IntObjectMap<String> localNamesSubMap = Objects.requireNonNullElse(localNames.get(i), IntObjectMaps.immutable.empty());
                                // first, parameters
                                for (ValType type : parameterTypes) {
                                    parameters.add(new Local(Objects.requireNonNullElse(localNamesSubMap.get(li), ""), type, li));
                                    li++;
                                }

                                int localCnt = cis.u32();
                                List<Local> locals;
                                if (localCnt > 0) {
                                    locals = new ArrayList<>();
                                    for (int j = 0; j < localCnt; j++) {
                                        int n = cis.u32();
                                        ValType type = cis.type();
                                        for (int k = 0; k < n; k++) {
                                            locals.add(new Local(Objects.requireNonNullElse(localNamesSubMap.get(li++), ""), type));
                                        }
                                    }
                                } else {
                                    locals = List.of();
                                }
                                DefinedFunc func = new DefinedFunc(Objects.requireNonNullElse(funcNames.get(i), ""), funcType, parameters, locals, insnSeqFactory.get());
                                funcBodies.put(func, cis);
                                defined.add(func);
                                funcs.add(func);
                            } catch (Throwable t) {
                                try {
                                    cis.close();
                                } catch (Throwable t2) {
                                    t.addSuppressed(t2);
                                }
                                throw t;
                            }
                        }
                    }
                }

                // Data
                int dataCnt = -1;
                if (sectionStreams.containsKey(Wasm.Section.DATA_COUNT)) {
                    try (WasmInputStream sis = sectionStreams.remove(Wasm.Section.DATA_COUNT)) {
                        dataCnt = sis.u32();
                    }
                }

                Map<ActiveSegment, Memory> memoryInitializers;
                List<Segment> segments;
                if (sectionStreams.containsKey(Wasm.Section.DATA)) {
                    try (WasmInputStream sis = sectionStreams.remove(Wasm.Section.DATA)) {
                        int cnt = sis.u32();
                        if (dataCnt != -1 && cnt != dataCnt) {
                            throw new IOException("Mismatched data count");
                        }
                        // conservative sizing
                        memoryInitializers = new HashMap<>(cnt);
                        segments = new ArrayList<>(cnt);
                        for (int i = 0; i < cnt; i++) {
                            Segment segment;
                            String name = Objects.requireNonNullElse(segmentNames.get(i), "");
                            switch (sis.u32()) {
                                case 0 -> {
                                    InsnSeq offset = constInsnSeqFactory.get();
                                    initializers.put(offset, sis.fork());
                                    InsnSeq.skip(sis);
                                    Data data = Data.of(sis.byteVec());
                                    Memory memory = memories.get(0);
                                    ActiveSegment as = new ActiveSegment(name, data, offset);
                                    memoryInitializers.put(as, memory);
                                    segment = as;
                                }
                                case 1 -> {
                                    Data data = Data.of(sis.byteVec());
                                    segment = new PassiveSegment(name, data);
                                }
                                case 2 -> {
                                    Memory memory = memories.get(sis.u32());
                                    InsnSeq offset = constInsnSeqFactory.get();
                                    initializers.put(offset, sis.fork());
                                    InsnSeq.skip(sis);
                                    Data data = Data.of(sis.byteVec());
                                    ActiveSegment as = new ActiveSegment(name, data, offset);
                                    memoryInitializers.put(as, memory);
                                    segment = as;
                                }
                                default -> throw new IOException("Invalid segment kind");
                            }
                            segments.add(segment);
                        }
                    }
                } else {
                    memoryInitializers = Map.of();
                    segments = List.of();
                }

                // after this point, everything is fully resolvable
                Resolver resolver = new Resolver() {
                    @Override
                    public BranchTarget resolveBranchTarget(int index) {
                        throw new IllegalArgumentException("Invalid branch target");
                    }

                    @Override
                    public Element resolveElement(int index) {
                        return elements.get(index);
                    }

                    @Override
                    public Func resolveFunc(int index) {
                        return funcs.get(index);
                    }

                    @Override
                    public FuncType resolveFuncType(int index) {
                        return types.get(index);
                    }

                    @Override
                    public Global resolveGlobal(int index) {
                        return globals.get(index);
                    }

                    @Override
                    public Local resolveLocal(int index) {
                        throw new IllegalArgumentException("Invalid local index");
                    }

                    @Override
                    public Memory resolveMemory(int index) {
                        return memories.get(index);
                    }

                    @Override
                    public Table resolveTable(int index) {
                        return tables.get(index);
                    }

                    @Override
                    public Segment resolveSegment(int index) {
                        return segments.get(index);
                    }

                    @Override
                    public Tag resolveTag(int index) {
                        return tags.get(index);
                    }
                };

                // Define the module
                Module module = new Module(moduleName);
                defined.forEach(module::define);
                imports.forEach(module::import_);
                for (Map.Entry<ActiveElement, Table> entry : tableInitializers.entrySet()) {
                    module.addInitializer(entry.getValue(), entry.getKey());
                }
                for (Map.Entry<ActiveSegment, Memory> entry : memoryInitializers.entrySet()) {
                    module.addInitializer(entry.getValue(), entry.getKey());
                }

                // Add exports
                if (sectionStreams.containsKey(Wasm.Section.EXPORT)) {
                    try (WasmInputStream sis = sectionStreams.remove(Wasm.Section.EXPORT)) {
                        int cnt = sis.u32();
                        for (int i = 0; i < cnt; i++) {
                            String name = sis.utf8();
                            module.export(name, switch (sis.u32()) {
                                case 0x00 -> resolver.resolveFunc(sis.u32());
                                case 0x01 -> resolver.resolveTable(sis.u32());
                                case 0x02 -> resolver.resolveMemory(sis.u32());
                                case 0x03 -> resolver.resolveGlobal(sis.u32());
                                case 0x04 -> resolver.resolveTag(sis.u32());
                                default -> throw new IOException("Invalid export type");
                            });
                        }
                    }
                }

                // Add start section
                if (sectionStreams.containsKey(Wasm.Section.START)) {
                    try (WasmInputStream sis = sectionStreams.remove(Wasm.Section.START)) {
                        module.startFunc(resolver.resolveFunc(sis.u32()));
                    }
                }

                // Lastly, resolve initializers
                funcListInitializers.forEachKeyValue((seq, idx) -> {
                    seq.add(Ops.ref.func, resolver.resolveFunc(idx));
                    seq.end();
                });
                for (final Iterator<Map.Entry<InsnSeq, WasmInputStream>> iterator = initializers.entrySet().iterator(); iterator.hasNext(); ) {
                    final Map.Entry<InsnSeq, WasmInputStream> entry = iterator.next();
                    try (WasmInputStream iis = entry.getValue()) {
                        entry.getKey().readFrom(iis, resolver);
                    }
                    iterator.remove();
                }
                for (Iterator<Map.Entry<DefinedFunc, WasmInputStream>> iter = funcBodies.entrySet().iterator(); iter.hasNext(); ) {
                    final Map.Entry<DefinedFunc, WasmInputStream> entry = iter.next();
                    try (WasmInputStream iis = entry.getValue()) {
                        DefinedFunc func = entry.getKey();
                        func.body().readFrom(iis, new Resolver.Delegating() {
                            @Override
                            public Resolver getDelegate() {
                                return resolver;
                            }

                            @Override
                            public Local resolveLocal(int index) {
                                int psz = func.parameters().size();
                                return index < psz ? func.parameters().get(index) : func.locals().get(index - psz);
                            }

                            @Override
                            public BranchTarget resolveBranchTarget(int index) {
                                return index == 0 ? func : getDelegate().resolveBranchTarget(index - 1);
                            }
                        });
                    }
                    iter.remove();
                }

                return module;
            } catch (Throwable t) {
                for (WasmInputStream stream : initializers.values()) {
                    try {
                        stream.close();
                    } catch (Throwable t2) {
                        t.addSuppressed(t2);
                    }
                }
                for (WasmInputStream stream : funcBodies.values()) {
                    try {
                        stream.close();
                    } catch (Throwable t2) {
                        t.addSuppressed(t2);
                    }
                }
                throw t;
            }
        } catch (Throwable t) {
            for (WasmInputStream value : sectionStreams.values()) {
                try {
                    value.close();
                } catch (Throwable t2) {
                    t.addSuppressed(t2);
                }
            }
            throw t;
        }
    }

    private static <E> void register(final List<E> list, final MutableObjectIntMap<E> idxs, final E item) {
        int idx = idxs.size();
        if (idx == idxs.getIfAbsentPut(item, idx)) {
            // it's new
            list.add(item);
            assert list.size() == idxs.size();
        }
    }

    private static <E> ArrayList<E> newArrayList(Object ignored) {
        return new ArrayList<>();
    }

    private static <E> HashSet<E> newSet(Object ignored) {
        return new HashSet<>(4);
    }

    private static void writeLimits(final WasmOutputStream sos, final Limits limits) throws IOException {
        long min = limits.minSize();
        long max = limits.maxSize();
        if (limits.shared()) {
            sos.rawByte(0x03);
            sos.u32(min);
            sos.u32(max);
        } else if (max == Wasm.LIMITS_MAXIMUM) {
            sos.rawByte(0x00);
            sos.u32(min);
        } else {
            sos.rawByte(0x01);
            sos.u32(min);
            sos.u32(max);
        }
    }
}

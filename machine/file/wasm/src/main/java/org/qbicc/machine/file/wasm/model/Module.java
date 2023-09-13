package org.qbicc.machine.file.wasm.model;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import io.smallrye.common.constraint.Assert;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.qbicc.machine.file.wasm.Data;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.Mutability;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.TagAttribute;
import org.qbicc.machine.file.wasm.ValType;
import org.qbicc.machine.file.wasm.Wasm;
import org.qbicc.machine.file.wasm.stream.ActiveDataVisitor;
import org.qbicc.machine.file.wasm.stream.ActiveElementVisitor;
import org.qbicc.machine.file.wasm.stream.CodeSectionVisitor;
import org.qbicc.machine.file.wasm.stream.CodeVisitor;
import org.qbicc.machine.file.wasm.stream.DataCountSectionVisitor;
import org.qbicc.machine.file.wasm.stream.DataSectionVisitor;
import org.qbicc.machine.file.wasm.stream.DataVisitor;
import org.qbicc.machine.file.wasm.stream.ElementInitVisitor;
import org.qbicc.machine.file.wasm.stream.ElementSectionVisitor;
import org.qbicc.machine.file.wasm.stream.ElementVisitor;
import org.qbicc.machine.file.wasm.stream.ExportSectionVisitor;
import org.qbicc.machine.file.wasm.stream.FunctionSectionVisitor;
import org.qbicc.machine.file.wasm.stream.GlobalSectionVisitor;
import org.qbicc.machine.file.wasm.stream.ImportSectionVisitor;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;
import org.qbicc.machine.file.wasm.stream.MemorySectionVisitor;
import org.qbicc.machine.file.wasm.stream.ModuleReader;
import org.qbicc.machine.file.wasm.stream.ModuleVisitor;
import org.qbicc.machine.file.wasm.stream.ModuleWriter;
import org.qbicc.machine.file.wasm.stream.StartSectionVisitor;
import org.qbicc.machine.file.wasm.stream.TableSectionVisitor;
import org.qbicc.machine.file.wasm.stream.TagSectionVisitor;
import org.qbicc.machine.file.wasm.stream.TypeSectionVisitor;

/**
 * A WASM module.
 * This API is not thread-safe.
 */
public final class Module {
    private final Set<Imported> imported = new LinkedHashSet<>();
    private final Map<String, Export<?>> exportsByName = new LinkedHashMap<>();
    private final Set<Defined> defined = new LinkedHashSet<>();
    private Func startFunc;

    /**
     * Construct a new empty model.
     */
    public Module() {
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
     * Import an item.
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
     * Iterate over each imported item.
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

    public void readFrom(Path path) throws IOException {
        try (ModuleReader wr = ModuleReader.forFile(path)) {
            readFrom(wr);
        }
    }

    public void readFrom(ModuleReader wr) throws IOException {
        wr.accept(visitor());
    }

    public void writeTo(Path path) throws IOException {
        try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writeTo(ModuleWriter.forStream(os));
        } catch (IOException e) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e2) {
                e.addSuppressed(e2);
            }
            throw e;
        }
    }

    public void writeTo(ModuleWriter ww) throws IOException {
        accept(ww);
    }

    /**
     * Get a visitor which can populate this model.
     *
     * @return the visitor (not {@code null})
     */
    public ModuleVisitor<RuntimeException> visitor() {
        return new ModuleVisitor<>() {
            private final MutableObjectIntMap<FuncType> typeIdxs = ObjectIntMaps.mutable.empty();
            private final List<FuncType> typesByIndex = new ArrayList<>();
            private final List<ElementHandle> elementsByIndex = new ArrayList<>();
            private final ArrayList<Func> funcsByIndex = new ArrayList<>();
            private final List<Table> tablesByIndex = new ArrayList<>();
            private final List<Memory> memoriesByIndex = new ArrayList<>();
            private final List<Global> globalsByIndex = new ArrayList<>();
            private final List<Tag> tagsByIndex = new ArrayList<>();
            private final List<SegmentHandle> segmentsByIndex = new ArrayList<>();
            private int importedFuncCount;

            private final Insn.Resolver resolver = new Insn.Resolver() {
                @Override
                public ElementHandle resolveElement(int index) {
                    while (index >= elementsByIndex.size()) {
                        elementsByIndex.add(ElementHandle.unresolved());
                    }
                    return elementsByIndex.get(index);
                }

                @Override
                public Func resolveFunc(int index) {
                    return funcsByIndex.get(index);
                }

                @Override
                public FuncType resolveFuncType(int index) {
                    return typesByIndex.get(index);
                }

                @Override
                public Global resolveGlobal(int index) {
                    return globalsByIndex.get(index);
                }

                @Override
                public Memory resolveMemory(int index) {
                    return memoriesByIndex.get(index);
                }

                @Override
                public Table resolveTable(int index) {
                    return tablesByIndex.get(index);
                }

                @Override
                public SegmentHandle resolveSegment(int index) {
                    while (index >= segmentsByIndex.size()) {
                        segmentsByIndex.add(SegmentHandle.unresolved());
                    }
                    return segmentsByIndex.get(index);
                }

                @Override
                public Tag resolveTag(int index) {
                    return tagsByIndex.get(index);
                }
            };

            @Override
            public TypeSectionVisitor<RuntimeException> visitTypeSection() throws RuntimeException {
                return new TypeSectionVisitor<>() {
                    @Override
                    public void visitType(FuncType functionType) throws RuntimeException {
                        typeIdxs.put(functionType, typesByIndex.size());
                        typesByIndex.add(functionType);
                    }
                };
            }

            @Override
            public ImportSectionVisitor<RuntimeException> visitImportSection() throws RuntimeException {
                return new ImportSectionVisitor<>() {
                    @Override
                    public void visitFunctionImport(String moduleName, String name, int typeIdx) throws RuntimeException {
                        ImportedFunc func = new ImportedFunc(moduleName, name, typesByIndex.get(typeIdx));
                        import_(func);
                        funcsByIndex.add(func);
                        importedFuncCount++;
                    }

                    @Override
                    public void visitTableImport(String moduleName, String name, RefType type, int min) throws RuntimeException {
                        ImportedTable table = new ImportedTable(moduleName, name, type, Integer.toUnsignedLong(min), new ArrayList<>(0));
                        import_(table);
                        tablesByIndex.add(table);
                    }

                    @Override
                    public void visitTableImport(String moduleName, String name, RefType type, int min, int max, boolean shared) throws RuntimeException {
                        ImportedTable table = new ImportedTable(moduleName, name, type, Integer.toUnsignedLong(min), Integer.toUnsignedLong(max), shared, new ArrayList<>(0));
                        import_(table);
                        tablesByIndex.add(table);
                    }

                    @Override
                    public void visitMemoryImport(String moduleName, String name, int min) throws RuntimeException {
                        ImportedMemory memory = new ImportedMemory(moduleName, name, Integer.toUnsignedLong(min), new ArrayList<>(0));
                        import_(memory);
                        memoriesByIndex.add(memory);
                    }

                    @Override
                    public void visitMemoryImport(String moduleName, String name, int min, int max, boolean shared) throws RuntimeException {
                        ImportedMemory memory = new ImportedMemory(moduleName, name, Integer.toUnsignedLong(min), Integer.toUnsignedLong(max), shared, new ArrayList<>(0));
                        import_(memory);
                        memoriesByIndex.add(memory);
                    }

                    @Override
                    public void visitGlobalImport(String moduleName, String name, ValType type, Mutability mut) throws RuntimeException {
                        ImportedGlobal global = new ImportedGlobal(moduleName, name, type, mut);
                        import_(global);
                        globalsByIndex.add(global);
                    }

                    @Override
                    public void visitTagImport(String moduleName, String name, TagAttribute attribute, int typeIdx) throws RuntimeException {
                        ImportedTag tag = new ImportedTag(moduleName, name, attribute, resolver.resolveFuncType(typeIdx));
                        import_(tag);
                        tagsByIndex.add(tag);
                    }
                };
            }

            @Override
            public FunctionSectionVisitor<RuntimeException> visitFunctionSection() throws RuntimeException {
                return new FunctionSectionVisitor<>() {
                    @Override
                    public void visitFunction(int typeIdx) throws RuntimeException {
                        DefinedFunc func = new DefinedFunc(typesByIndex.get(typeIdx), new ArrayList<>(), new InsnSeq());
                        define(func);
                        funcsByIndex.add(func);
                    }
                };
            }

            @Override
            public TableSectionVisitor<RuntimeException> visitTableSection() throws RuntimeException {
                return new TableSectionVisitor<>() {
                    @Override
                    public void visitTable(RefType type, int min) throws RuntimeException {
                        DefinedTable table = new DefinedTable(type, Integer.toUnsignedLong(min), new ArrayList<>());
                        define(table);
                        tablesByIndex.add(table);
                    }

                    @Override
                    public void visitTable(RefType type, int min, int max) throws RuntimeException {
                        DefinedTable table = new DefinedTable(type, Integer.toUnsignedLong(min), Integer.toUnsignedLong(max), new ArrayList<>());
                        define(table);
                        tablesByIndex.add(table);
                    }
                };
            }

            @Override
            public MemorySectionVisitor<RuntimeException> visitMemorySection() throws RuntimeException {
                return new MemorySectionVisitor<>() {
                    @Override
                    public void visitMemory(int min) throws RuntimeException {
                        DefinedMemory memory = new DefinedMemory(Integer.toUnsignedLong(min), new ArrayList<>());
                        define(memory);
                        memoriesByIndex.add(memory);
                    }

                    @Override
                    public void visitMemory(int min, int max) throws RuntimeException {
                        DefinedMemory memory = new DefinedMemory(Integer.toUnsignedLong(min), Integer.toUnsignedLong(max), new ArrayList<>());
                        define(memory);
                        memoriesByIndex.add(memory);
                    }
                };
            }

            @Override
            public GlobalSectionVisitor<RuntimeException> visitGlobalSection() throws RuntimeException {
                return new GlobalSectionVisitor<>() {
                    @Override
                    public InsnSeqVisitor<RuntimeException> visitGlobal(ValType type, Mutability mut) throws RuntimeException {
                        InsnSeq seq = new InsnSeq();
                        return new InsnSeq.PopulatingVisitor<>(seq, resolver) {
                            @Override
                            public void visitEnd() throws RuntimeException {
                                super.visitEnd();
                                DefinedGlobal global = new DefinedGlobal(type, mut, seq);
                                define(global);
                                globalsByIndex.add(global);
                            }
                        };
                    }
                };
            }

            @Override
            public ExportSectionVisitor<RuntimeException> visitExportSection() throws RuntimeException {
                return new ExportSectionVisitor<>() {
                    @Override
                    public void visitFunctionExport(String name, int funcIdx) throws RuntimeException {
                        export(name, funcsByIndex.get(funcIdx));
                    }

                    @Override
                    public void visitTableExport(String name, int tableIdx) throws RuntimeException {
                        export(name, tablesByIndex.get(tableIdx));
                    }

                    @Override
                    public void visitMemoryExport(String name, int memIdx) throws RuntimeException {
                        export(name, memoriesByIndex.get(memIdx));
                    }

                    @Override
                    public void visitGlobalExport(String name, int globalIdx) throws RuntimeException {
                        export(name, globalsByIndex.get(globalIdx));
                    }
                };
            }

            @Override
            public StartSectionVisitor<RuntimeException> visitStartSection() throws RuntimeException {
                return new StartSectionVisitor<>() {
                    @Override
                    public void visitStartFunction(int index) throws RuntimeException {
                        startFunc(funcsByIndex.get(index));
                    }
                };
            }

            @Override
            public ElementSectionVisitor<RuntimeException> visitElementSection() throws RuntimeException {
                return new ElementSectionVisitor<>() {
                    private int index;

                    private ElementVisitor<RuntimeException> visitGenericPassiveElement(BiFunction<RefType, ElementInit, Element> ender) {
                        return new ElementVisitor<>() {
                            RefType type;
                            ElementInit init;

                            @Override
                            public void visitType(RefType refType) throws RuntimeException {
                                type = refType;
                            }

                            @Override
                            public void visitInit(int... funcIdx) throws RuntimeException {
                                List<Func> list = new ArrayList<>(funcIdx.length);
                                for (final int idx : funcIdx) {
                                    list.add(funcsByIndex.get(idx));
                                }
                                init = new FuncListElementInit(list);
                            }

                            @Override
                            public ElementInitVisitor<RuntimeException> visitInit() throws RuntimeException {
                                return new ElementInitVisitor<>() {
                                    private final List<InsnSeq> exprs = new ArrayList<>();

                                    @Override
                                    public InsnSeqVisitor<RuntimeException> visitInit() {
                                        InsnSeq seq = new InsnSeq();
                                        exprs.add(seq);
                                        return new InsnSeq.PopulatingVisitor<>(seq, resolver);
                                    }

                                    @Override
                                    public void visitEnd() throws RuntimeException {
                                        init = new ExprElementInit(exprs);
                                    }
                                };
                            }

                            @Override
                            public void visitEnd() throws RuntimeException {
                                Element element = ender.apply(type, init);
                                resolver.resolveElement(index++).initialize(element);
                            }
                        };
                    }

                    @Override
                    public ElementVisitor<RuntimeException> visitPassiveElement() throws RuntimeException {
                        return visitGenericPassiveElement(PassiveElement::new);
                    }

                    @Override
                    public ElementVisitor<RuntimeException> visitDeclarativeElement() throws RuntimeException {
                        return visitGenericPassiveElement(DeclarativeElement::new);
                    }

                    @Override
                    public ActiveElementVisitor<RuntimeException> visitActiveElement() throws RuntimeException {
                        return new ActiveElementVisitor<>() {
                            private Table table;
                            private InsnSeq offset;
                            private RefType type;
                            private ElementInit init;

                            @Override
                            public void visitTableIndex(int index) throws RuntimeException {
                                table = tablesByIndex.get(index);
                            }

                            @Override
                            public InsnSeqVisitor<RuntimeException> visitOffset() throws RuntimeException {
                                InsnSeq seq = new InsnSeq();
                                offset = seq;
                                return new InsnSeq.PopulatingVisitor<>(seq, resolver);
                            }

                            @Override
                            public void visitType(RefType refType) throws RuntimeException {
                                type = refType;
                            }

                            @Override
                            public void visitInit(int... funcIdx) throws RuntimeException {
                                List<Func> list = new ArrayList<>(funcIdx.length);
                                for (final int idx : funcIdx) {
                                    list.add(funcsByIndex.get(idx));
                                }
                                init = new FuncListElementInit(list);
                            }

                            @Override
                            public ElementInitVisitor<RuntimeException> visitInit() throws RuntimeException {
                                return new ElementInitVisitor<>() {
                                    private final List<InsnSeq> exprs = new ArrayList<>();

                                    @Override
                                    public InsnSeqVisitor<RuntimeException> visitInit() {
                                        InsnSeq seq = new InsnSeq();
                                        exprs.add(seq);
                                        return new InsnSeq.PopulatingVisitor<>(seq, resolver);
                                    }

                                    @Override
                                    public void visitEnd() throws RuntimeException {
                                        init = new ExprElementInit(exprs);
                                    }
                                };
                            }

                            @Override
                            public void visitEnd() throws RuntimeException {
                                ActiveElement element = new ActiveElement(offset, type, init);
                                table.initializers().add(element);
                                resolver.resolveElement(index++).initialize(element);
                            }
                        };
                    }
                };
            }

            @Override
            public CodeSectionVisitor<RuntimeException> visitCodeSection() throws RuntimeException {
                return new CodeSectionVisitor<>() {
                    int index = importedFuncCount;
                    @Override
                    public CodeVisitor<RuntimeException> visitCode() throws RuntimeException {
                        DefinedFunc func = (DefinedFunc) funcsByIndex.get(index++);
                        return new CodeVisitor<>() {

                            @Override
                            public void visitLocal(int count, ValType type) throws RuntimeException {
                                if (count == 1) {
                                    func.localTypes().add(type);
                                } else {
                                    func.localTypes().addAll(Collections.nCopies(count, type));
                                }
                            }

                            @Override
                            public InsnSeqVisitor<RuntimeException> visitBody() throws RuntimeException {
                                return new InsnSeq.PopulatingVisitor<>(func.body(), resolver, func);
                            }

                            @Override
                            public void visitEnd() throws RuntimeException {
                            }
                        };
                    }
                };
            }

            @Override
            public TagSectionVisitor<RuntimeException> visitTagSection() throws RuntimeException {
                return new TagSectionVisitor<>() {
                    @Override
                    public void visitTag(TagAttribute attribute, int typeIdx) throws RuntimeException {
                        DefinedTag tag = new DefinedTag(attribute, resolver.resolveFuncType(typeIdx));
                        define(tag);
                        tagsByIndex.add(tag);
                    }
                };
            }

            @Override
            public DataSectionVisitor<RuntimeException> visitDataSection() throws RuntimeException {
                return new DataSectionVisitor<>() {
                    private int index = 0;
                    @Override
                    public DataVisitor<RuntimeException> visitPassiveSegment() throws RuntimeException {
                        return new DataVisitor<>() {
                            @Override
                            public void visitData(Data data) throws RuntimeException {
                                Segment segment = new PassiveSegment(data);
                                resolver.resolveSegment(index).initialize(segment);
                            }

                            @Override
                            public void visitEnd() throws RuntimeException {
                                super.visitEnd();
                                index ++;
                            }
                        };
                    }

                    @Override
                    public ActiveDataVisitor<RuntimeException> visitActiveSegment(int memIdx) throws RuntimeException {
                        return new ActiveDataVisitor<>() {
                            private final InsnSeq offset = new InsnSeq();
                            private Data data;

                            @Override
                            public InsnSeqVisitor<RuntimeException> visitOffset() throws RuntimeException {
                                return new InsnSeq.PopulatingVisitor<>(offset, resolver);
                            }

                            @Override
                            public void visitData(Data data) throws RuntimeException {
                                this.data = data;
                            }

                            @Override
                            public void visitEnd() throws RuntimeException {
                                super.visitEnd();
                                Memory memory = resolver.resolveMemory(memIdx);
                                ActiveSegment segment = new ActiveSegment(data, offset);
                                resolver.resolveSegment(index).initialize(segment);
                                memory.initializers().add(segment);
                                index ++;
                            }
                        };
                    }
                };
            }
        };
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

    /**
     * Write this module using the given visitor.
     *
     * @param visitor the visitor
     * @param <E> the exception type
     * @throws E if the visitor throws an exception
     */
    public <E extends Exception> void accept(ModuleVisitor<E> visitor) throws E {
        if (visitor == null) {
            return;
        }
        // all index spaces
        // types
        MutableObjectIntMap<FuncType> typeIdxs = ObjectIntMaps.mutable.empty();
        List<FuncType> types = new ArrayList<>();
        // functions
        MutableObjectIntMap<Func> funcIdxs = ObjectIntMaps.mutable.empty();
        int funcCnt = 0;
        List<DefinedFunc> definedFuncs = new ArrayList<>();
        // tables
        MutableObjectIntMap<Table> tableIdxs = ObjectIntMaps.mutable.empty();
        int tableCnt = 0;
        List<DefinedTable> definedTables = new ArrayList<>();
        // memories
        MutableObjectIntMap<Memory> memoryIdxs = ObjectIntMaps.mutable.empty();
        int memoryCnt = 0;
        List<DefinedMemory> definedMemories = new ArrayList<>();
        // globals
        MutableObjectIntMap<Global> globalIdxs = ObjectIntMaps.mutable.empty();
        int globalCnt = 0;
        List<DefinedGlobal> definedGlobals = new ArrayList<>();
        // tags
        MutableObjectIntMap<Tag> tagIdxs = ObjectIntMaps.mutable.empty();
        int tagCnt = 0;
        List<DefinedTag> definedTags = new ArrayList<>();
        // elements
        MutableObjectIntMap<Element> elementIdxs = ObjectIntMaps.mutable.empty();
        List<Element> elements = new ArrayList<>();
        Map<ActiveElement, Table> activeElements = new HashMap<>();
        // data
        MutableObjectIntMap<Segment> segmentIdxs = ObjectIntMaps.mutable.empty();
        List<Segment> segments = new ArrayList<>();
        Map<ActiveSegment, Memory> activeSegments = new HashMap<>();

        Insn.Encoder encoder = new Insn.Encoder() {
            @Override
            public int encode(BranchTarget branchTarget) {
                if (branchTarget instanceof DefinedFunc) {
                    // all the way out
                    return 0;
                } else {
                    throw new NoSuchElementException("No block index for " + branchTarget);
                }
            }

            @Override
            public int encode(Element element) {
                return elementIdxs.getOrThrow(element);
            }

            @Override
            public int encode(Func func) {
                return funcIdxs.getOrThrow(func);
            }

            @Override
            public int encode(FuncType type) {
                return typeIdxs.getOrThrow(type);
            }

            @Override
            public int encode(Global global) {
                return globalIdxs.getOrThrow(global);
            }

            @Override
            public int encode(Memory memory) {
                return memoryIdxs.getOrThrow(memory);
            }

            @Override
            public int encode(Table table) {
                return tableIdxs.getOrThrow(table);
            }

            @Override
            public int encode(Segment seg) {
                return segmentIdxs.getOrThrow(seg);
            }

            @Override
            public int encode(Tag tag) {
                return tagIdxs.getOrThrow(tag);
            }
        };

        // populate tables; imported items first
        for (Imported item : imported) {
            // todo: type switch
            if (item instanceof ImportedFunc ifn) {
                funcIdxs.put(ifn, funcCnt++);
                getOrRegister(ifn.type(), typeIdxs, types);
            } else if (item instanceof ImportedGlobal ig) {
                globalIdxs.put(ig, globalCnt++);
            } else if (item instanceof ImportedMemory im) {
                memoryIdxs.put(im, memoryCnt++);
                for (ActiveSegment initializer : im.initializers()) {
                    segmentIdxs.getIfAbsentPut(initializer, segmentIdxs.size());
                    activeSegments.put(initializer, im);
                    segments.add(initializer);
                }
            } else if (item instanceof ImportedTable it) {
                tableIdxs.put(it, tableCnt++);
                for (ActiveElement initializer : it.initializers()) {
                    elementIdxs.getIfAbsentPut(initializer, elementIdxs.size());
                    activeElements.put(initializer, it);
                    elements.add(initializer);
                }
            } else {
                throw new IllegalStateException();
            }
        }
        // next, defined items
        for (Defined item : defined) {
            // todo: type switch
            if (item instanceof DefinedFunc df) {
                funcIdxs.put(df, funcCnt++);
                definedFuncs.add(df);
                getOrRegister(df.type(), typeIdxs, types);
                // scan for segments and elements
                df.body().forEachRecursive(insn -> {
                    if (insn instanceof BlockInsn bi) {
                        getOrRegister(bi.type(), typeIdxs, types);
                    } else if (insn instanceof DataInsn di) {
                        int size = segmentIdxs.size();
                        if (size == segmentIdxs.getIfAbsentPut(di.segment(), size)) {
                            segments.add(di.segment());
                        }
                    } else if (insn instanceof MemoryAndDataInsn di) {
                        int size = segmentIdxs.size();
                        if (size == segmentIdxs.getIfAbsentPut(di.segment(), size)) {
                            segments.add(di.segment());
                        }
                    } else if (insn instanceof ElementInsn ei) {
                        int size = elementIdxs.size();
                        if (size == elementIdxs.getIfAbsentPut(ei.element(), size)) {
                            elements.add(ei.element());
                        }
                    } else if (insn instanceof ElementAndTableInsn ei) {
                        int size = elementIdxs.size();
                        if (size == elementIdxs.getIfAbsentPut(ei.element(), size)) {
                            elements.add(ei.element());
                        }
                    } else if (insn instanceof TableAndFuncTypeInsn ti) {
                        getOrRegister(ti.type(), typeIdxs, types);
                    }
                });
            } else if (item instanceof DefinedGlobal dg) {
                globalIdxs.put(dg, globalCnt++);
                definedGlobals.add(dg);
            } else if (item instanceof DefinedMemory dm) {
                memoryIdxs.put(dm, memoryCnt++);
                definedMemories.add(dm);
                for (ActiveSegment initializer : dm.initializers()) {
                    segmentIdxs.getIfAbsentPut(initializer, segmentIdxs.size());
                    activeSegments.put(initializer, dm);
                }
            } else if (item instanceof DefinedTable dt) {
                tableIdxs.put(dt, tableCnt++);
                definedTables.add(dt);
                for (ActiveElement initializer : dt.initializers()) {
                    elementIdxs.getIfAbsentPut(initializer, elementIdxs.size());
                    activeElements.put(initializer, dt);
                }
            } else if (item instanceof DefinedTag dt) {
                tagIdxs.put(dt, tagCnt++);
                definedTags.add(dt);
                getOrRegister(dt.type(), typeIdxs, types);
            } else {
                throw new IllegalStateException();
            }
        }

        // visit each section in order (if needed)
        if (! types.isEmpty()) {
            TypeSectionVisitor<E> tv = visitor.visitTypeSection();
            if (tv != null) {
                for (FuncType type : types) {
                    tv.visitType(type);
                }
                tv.visitEnd();
            }
        }
        if (! imported.isEmpty()) {
            ImportSectionVisitor<E> iv = visitor.visitImportSection();
            if (iv != null) {
                for (Imported item : imported) {
                    // todo: type switch
                    if (item instanceof ImportedFunc ifn) {
                        iv.visitFunctionImport(ifn.moduleName(), ifn.name(), typeIdxs.getOrThrow(ifn.type()));
                    } else if (item instanceof ImportedGlobal ig) {
                        iv.visitGlobalImport(ig.moduleName(), ig.name(), ig.type(), ig.mutability());
                    } else if (item instanceof ImportedMemory im) {
                        if (im.maxSize() == Wasm.LIMITS_MAXIMUM) {
                            iv.visitMemoryImport(im.moduleName(), im.name(), (int) im.minSize());
                        } else {
                            iv.visitMemoryImport(im.moduleName(), im.name(), (int) im.minSize());
                        }
                    } else if (item instanceof ImportedTable it) {
                        if (it.maxSize() == Wasm.LIMITS_MAXIMUM) {
                            iv.visitTableImport(it.moduleName(), it.name(), it.type(), (int) it.minSize());
                        } else {
                            iv.visitTableImport(it.moduleName(), it.name(), it.type(), (int) it.minSize(), (int) it.maxSize(), it.shared());
                        }
                    } else if (item instanceof ImportedTag it) {
                        iv.visitTagImport(it.moduleName(), it.name(), it.attribute(), typeIdxs.getOrThrow(it.type()));
                    } else {
                        throw new IllegalStateException();
                    }
                }
                iv.visitEnd();
            }
        }
        if (! definedFuncs.isEmpty()) {
            FunctionSectionVisitor<E> fv = visitor.visitFunctionSection();
            if (fv != null) {
                for (DefinedFunc definedFunc : definedFuncs) {
                    fv.visitFunction(typeIdxs.getOrThrow(definedFunc.type()));
                }
                fv.visitEnd();
            }
        }
        if (! definedTables.isEmpty()) {
            TableSectionVisitor<E> tv = visitor.visitTableSection();
            if (tv != null) {
                for (DefinedTable definedTable : definedTables) {
                    if (definedTable.maxSize() == Wasm.LIMITS_MAXIMUM) {
                        tv.visitTable(definedTable.type(), (int) definedTable.minSize());
                    } else {
                        tv.visitTable(definedTable.type(), (int) definedTable.minSize(), (int) definedTable.maxSize());
                    }
                }
                tv.visitEnd();
            }
        }
        if (! definedMemories.isEmpty()) {
            MemorySectionVisitor<E> mv = visitor.visitMemorySection();
            if (mv != null) {
                for (DefinedMemory definedMemory : definedMemories) {
                    if (definedMemory.maxSize() == Wasm.LIMITS_MAXIMUM) {
                        mv.visitMemory((int) definedMemory.minSize());
                    } else {
                        mv.visitMemory((int) definedMemory.minSize(), (int) definedMemory.maxSize());
                    }
                }
                mv.visitEnd();
            }
        }
        if (! definedTags.isEmpty()) {
            TagSectionVisitor<E> tv = visitor.visitTagSection();
            if (tv != null) {
                for (DefinedTag definedTag : definedTags) {
                    tv.visitTag(definedTag.attribute(), encoder.encode(definedTag.type()));
                }
            }
        }
        if (! definedGlobals.isEmpty()) {
            GlobalSectionVisitor<E> gv = visitor.visitGlobalSection();
            if (gv != null) {
                for (DefinedGlobal definedGlobal : definedGlobals) {
                    InsnSeqVisitor<E> isv = gv.visitGlobal(definedGlobal.type(), definedGlobal.mutability());
                    if (isv != null) {
                        definedGlobal.init().accept(isv, encoder);
                        isv.visitEnd();
                    }
                }
                gv.visitEnd();
            }
        }
        if (! exportsByName.isEmpty()) {
            ExportSectionVisitor<E> ev = visitor.visitExportSection();
            if (ev != null) {
                for (Export<?> export : exportsByName.values()) {
                    Exportable item = export.exported();
                    if (item instanceof Func f) {
                        ev.visitFunctionExport(export.name(), funcIdxs.getOrThrow(f));
                    } else if (item instanceof Global g) {
                        ev.visitGlobalExport(export.name(), globalIdxs.getOrThrow(g));
                    } else if (item instanceof Memory m) {
                        ev.visitMemoryExport(export.name(), memoryIdxs.getOrThrow(m));
                    } else if (item instanceof Table t) {
                        ev.visitTableExport(export.name(), tableIdxs.getOrThrow(t));
                    } else {
                        throw new IllegalStateException();
                    }
                }
                ev.visitEnd();
            }
        }
        if (startFunc != null) {
            StartSectionVisitor<E> sv = visitor.visitStartSection();
            if (sv != null) {
                sv.visitStartFunction(funcIdxs.getOrThrow(startFunc));
                sv.visitEnd();
            }
        }
        if (! elements.isEmpty()) {
            ElementSectionVisitor<E> sv = visitor.visitElementSection();
            if (sv != null) {
                for (Element element : elements) {
                    ElementVisitor<E> ev;
                    if (element instanceof PassiveElement) {
                        ev = sv.visitPassiveElement();
                    } else if (element instanceof DeclarativeElement) {
                        ev = sv.visitDeclarativeElement();
                    } else if (element instanceof ActiveElement ae) {
                        ActiveElementVisitor<E> aev = sv.visitActiveElement();
                        aev.visitTableIndex(tableIdxs.getOrThrow(activeElements.get(ae)));
                        InsnSeqVisitor<E> isv = aev.visitOffset();
                        if (isv != null) {
                            ae.offset().accept(isv, encoder);
                            isv.visitEnd();
                        }
                        ev = aev;
                    } else {
                        throw new IllegalStateException();
                    }
                    if (ev != null) {
                        ev.visitType(element.type());
                        ElementInit init = element.init();
                        if (init instanceof ExprElementInit ei) {
                            ElementInitVisitor<E> isv = Objects.requireNonNullElseGet(ev.visitInit(), ElementInitVisitor::new);
                            for (InsnSeq item : ei.items()) {
                                InsnSeqVisitor<E> initItem = Objects.requireNonNullElseGet(isv.visitInit(), InsnSeqVisitor::new);
                                item.accept(initItem, encoder);
                                initItem.visitEnd();
                            }
                            isv.visitEnd();
                        } else if (init instanceof FuncListElementInit fi) {
                            List<Func> initItems = fi.items();
                            int[] idxs = new int[initItems.size()];
                            for (int i = 0; i < initItems.size(); i++) {
                                final Func initItem = initItems.get(i);
                                idxs[i] = funcIdxs.getOrThrow(initItem);
                            }
                            ev.visitInit(idxs);
                        } else {
                            throw new IllegalStateException();
                        }
                        ev.visitEnd();
                    }
                }
                sv.visitEnd();
            }
        }
        if (! segments.isEmpty()) {
            DataCountSectionVisitor<E> dv = visitor.visitDataCountSection();
            if (dv != null) {
                dv.visitDataCount(segments.size());
                dv.visitEnd();
            }
        }
        if (! definedFuncs.isEmpty()) {
            CodeSectionVisitor<E> csv = visitor.visitCodeSection();
            if (csv != null) {
                for (DefinedFunc df : definedFuncs) {
                    CodeVisitor<E> cv = csv.visitCode();
                    if (cv != null) {
                        ValType last = null;
                        int cnt = 0;
                        for (ValType vt : df.localTypes()) {
                            if (last == null) {
                                last = vt;
                                cnt = 1;
                            } else if (vt == last) {
                                cnt ++;
                            } else {
                                cv.visitLocal(cnt, last);
                                cnt = 1;
                                last = vt;
                            }
                        }
                        if (cnt > 0) {
                            cv.visitLocal(cnt, last);
                        }
                        InsnSeqVisitor<E> ev = cv.visitBody();
                        if (ev != null) {
                            df.body().accept(ev, encoder);
                            ev.visitEnd();
                        }
                        cv.visitEnd();
                    }
                }

                csv.visitEnd();
            }
        }
        if (! segments.isEmpty()) {
            DataSectionVisitor<E> dsv = visitor.visitDataSection();
            if (dsv != null) {
                for (Segment segment : segments) {
                    if (segment instanceof ActiveSegment as) {
                        ActiveDataVisitor<E> adv = dsv.visitActiveSegment(memoryIdxs.getOrThrow(activeSegments.get(as)));
                        if (adv != null) {
                            InsnSeqVisitor<E> ev = adv.visitOffset();
                            if (ev != null) {
                                as.init().accept(ev, encoder);
                                ev.visitEnd();
                            }
                            adv.visitData(as.data());
                            adv.visitEnd();
                        }
                    } else if (segment instanceof PassiveSegment ps) {
                        DataVisitor<E> dv = dsv.visitPassiveSegment();
                        if (dv != null) {
                            dv.visitData(ps.data());
                            dv.visitEnd();
                        }
                    } else {
                        throw new IllegalStateException();
                    }
                }
                dsv.visitEnd();
            }
        }
        visitor.visitEnd();
    }

    public static Module create(ModuleReader reader) throws IOException {
        Module module = new Module();
        module.readFrom(reader);
        return module;
    }

    private static <E> int getOrRegister(E item, MutableObjectIntMap<? super E> map, List<? super E> list) {
        int size = list.size();
        int result = map.getIfAbsentPut(item, size);
        if (result == size) {
            list.add(item);
        }
        return result;
    }
}

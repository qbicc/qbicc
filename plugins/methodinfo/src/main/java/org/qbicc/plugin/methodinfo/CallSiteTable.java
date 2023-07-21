package org.qbicc.plugin.methodinfo;

import static org.qbicc.runtime.stackwalk.CallSiteTable.*;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToIntFunction;

import io.smallrye.common.constraint.Assert;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.factory.primitive.ShortObjectMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.MutableShortObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.object.Data;
import org.qbicc.object.Function;
import org.qbicc.object.Linkage;
import org.qbicc.object.ModuleSection;
import org.qbicc.object.ProgramModule;
import org.qbicc.plugin.methodinfo.valueinfo.ConstantValueInfo;
import org.qbicc.plugin.methodinfo.valueinfo.FrameOffsetValueInfo;
import org.qbicc.plugin.methodinfo.valueinfo.RegisterRelativeValueInfo;
import org.qbicc.plugin.methodinfo.valueinfo.RegisterValueInfo;
import org.qbicc.plugin.methodinfo.valueinfo.ValueInfo;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.StructType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeIdType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.NamedElement;

/**
 * Utilities for constructing the call site instruction table.
 * Each call site in this table has information recorded for use by garbage collection and stack walking.
 */
public final class CallSiteTable {
    private static final AttachmentKey<CallSiteTable> KEY = new AttachmentKey<>();

    private final Map<ExecutableElement, SubprogramEntry> subprogramEntries = new ConcurrentHashMap<>();
    private final Map<LoadedTypeDefinition, List<CallSiteEntry>> entries = new ConcurrentHashMap<>();
    private final Map<ValueInfo, ValueInfo> valueInfos = new ConcurrentHashMap<>();
    private final Map<SourceCodeEntry, SourceCodeEntry> sourceCodeEntries = new ConcurrentHashMap<>();
    private final Map<Set<ValueInfo>, LiveValueInfo> liveValueInfos = new ConcurrentHashMap<>();

    private final CompilationContext ctxt;

    CallSiteTable(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    /**
     * Ensure that each element has a method type object during {@code ADD}.
     *
     * @param ee the executable element (must not be {@code null})
     */
    public static void computeMethodType(ExecutableElement ee) {
        Vm.requireCurrent().createMethodType(ee);
    }

    /**
     * Get the instruction table instance for the compilation context.
     *
     * @param ctxt the context (must not be {@code null})
     * @return the instruction table instance (not {@code null})
     */
    public static CallSiteTable get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, () -> new CallSiteTable(ctxt));
    }

    /**
     * Write the call site table data to a program module during {@code GENERATE}.
     * The back end must then perform any required compilation.
     *
     * @param ctxt the compilation context (must not be {@code null})
     */
    public static void writeCallSiteTable(CompilationContext ctxt) {
        get(ctxt).doWrite();
    }

    private void doWrite() {
        final LiteralFactory lf = ctxt.getLiteralFactory();
        final TypeSystem ts = ctxt.getTypeSystem();
        // TODO: this should be lowered directly to assembly or an object file
        // ----
        // set up the string and method type tables
        // ----
        ReferenceType stringType = ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load().getClassType().getReference();
        ReferenceType methodTypeType = ctxt.getBootstrapClassContext().findDefinedType("java/lang/invoke/MethodType").load().getClassType().getReference();
        MutableObjectIntMap<VmString> fileNamesTable = ObjectIntMaps.mutable.empty();
        MutableObjectIntMap<VmString> methodNamesTable = ObjectIntMaps.mutable.empty();
        MutableObjectIntMap<VmObject> methodTypesTable = ObjectIntMaps.mutable.empty();
        List<Literal> fileNamesList = new ArrayList<>();
        List<Literal> methodNamesList = new ArrayList<>();
        List<Literal> methodTypesList = new ArrayList<>();

        // ----
        // compute the LVI table, compacting entries with a common suffix
        // ----
        final TrieNode root = new TrieNode(null, (short)0);
        final ShortArrayList sal = new ShortArrayList();
        for (LiveValueInfo info : liveValueInfos.values()) {
            emitLiveValueInfo(info, sal);
            // use size-1 to exclude the 0 at the end, which is implicit in the root trie node
            root.findOrAdd(sal, info, sal.size() - 1);
            sal.clear();
        }
        // now, emit a compact list using the suffixMap
        MutableObjectIntMap<LiveValueInfo> lvtOffsets = ObjectIntMaps.mutable.empty();
        root.writeAllTo(sal, lvtOffsets);
        // now make it into a literal
        final Literal lviLiteral = lf.literalOf(ts.getArrayType(ts.getUnsignedInteger16Type(), sal.size()), sal.toArray());
        // ----
        // compute the subprogram table
        // ----
        List<SubprogramEntry> seList = new ArrayList<>(subprogramEntries.values());
        List<Literal> seLiterals = new ArrayList<>(seList.size());
        seList.sort(Comparator.comparingInt(SubprogramEntry::typeId).thenComparingInt(SubprogramEntry::methodIdx));
        StructType steType = (StructType) ctxt.getBootstrapClassContext().resolveTypeFromClassName("org/qbicc/runtime/stackwalk", "CallSiteTable$struct_subprogram");
        MutableObjectIntMap<SubprogramEntry> subprogramIndexes = ObjectIntMaps.mutable.empty();
        for (SubprogramEntry entry : seList) {
            subprogramIndexes.put(entry, subprogramIndexes.size());
            seLiterals.add(emitSubprogramEntry(
                steType,
                entry,
                obj -> lookUpItem(fileNamesTable, fileNamesList, obj),
                obj -> lookUpItem(methodNamesTable, methodNamesList, obj),
                obj -> lookUpItem(methodTypesTable, methodTypesList, obj)
            ));
        }
        final Literal seLiteral = lf.literalOf(ts.getArrayType(steType, seLiterals.size()), seLiterals);
        // ----
        // compute the source table
        // ----
        StructType sceType = (StructType) ctxt.getBootstrapClassContext().resolveTypeFromClassName("org/qbicc/runtime/stackwalk", "CallSiteTable$struct_source");
        List<Literal> sceLiterals = new ArrayList<>(sourceCodeEntries.size());
        MutableObjectIntMap<SourceCodeEntry> sourceCodeIndexes = ObjectIntMaps.mutable.empty();
        for (SourceCodeEntry entry : sourceCodeEntries.values()) {
            getSceIndex(sceType, entry, sceLiterals, sourceCodeIndexes, subprogramIndexes);
        }
        final Literal sceLiteral = lf.literalOf(ts.getArrayType(sceType, sceLiterals.size()), sceLiterals);
        // ----
        // compute the call site table
        // ----
        StructType csType = (StructType) ctxt.getBootstrapClassContext().resolveTypeFromClassName("org/qbicc/runtime/stackwalk", "CallSiteTable$struct_call_site");
        final ArrayList<Map.Entry<LoadedTypeDefinition, List<CallSiteEntry>>> callSiteListList = new ArrayList<>(entries.entrySet());
        // estimate size
        List<Literal> csLiterals = new ArrayList<>(callSiteListList.size() * 16);
        callSiteListList.sort(Comparator.comparingInt(e -> e.getKey().getTypeId()));
        for (Map.Entry<LoadedTypeDefinition, List<CallSiteEntry>> entry : callSiteListList) {
            for (CallSiteEntry callSiteEntry : entry.getValue()) {
                csLiterals.add(emitCallSiteEntry(csType, callSiteEntry, sourceCodeIndexes::get, key -> {
                    final int lvo = lvtOffsets.getIfAbsent(key, -1);
                    if (lvo == -1) {
                        throw new IllegalStateException("Missing LVT entry");
                    }
                    return lvo;
                }));
            }
        }
        final Literal csLiteral = lf.literalOf(ts.getArrayType(csType, csLiterals.size()), csLiterals);
        final Literal csSizeLiteral = lf.literalOf(ts.getUnsignedInteger64Type(), csLiterals.size());
        final Literal fileNamesLiteral = lf.literalOf(ts.getArrayType(stringType, fileNamesList.size()), fileNamesList);
        final Literal methodNamesLiteral = lf.literalOf(ts.getArrayType(stringType, methodNamesList.size()), methodNamesList);
        final Literal methodTypesLiteral = lf.literalOf(ts.getArrayType(methodTypeType, methodTypesList.size()), methodTypesList);
        // create a second program module for the call site table
        final LoadedTypeDefinition cstTypeDef = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/stackwalk/CallSiteTable").load();
        final DefinedTypeDefinition defaultTypeDef = ctxt.getDefaultTypeDefinition();
        final ProgramModule programModule = ctxt.getOrAddProgramModule(defaultTypeDef);
        final ModuleSection moduleSection = programModule.inSection(ctxt.getImplicitSection());
        final FieldElement callSiteTbl = cstTypeDef.findField("call_site_tbl");
        final FieldElement callSiteTblSize = cstTypeDef.findField("call_site_tbl_size");
        final FieldElement sourceTbl = cstTypeDef.findField("source_tbl");
        final FieldElement subprogramTbl = cstTypeDef.findField("subprogram_tbl");
        final FieldElement lviTbl = cstTypeDef.findField("lvi_tbl");
        final FieldElement fileNameRefs = cstTypeDef.findField("file_name_refs");
        final FieldElement methodNameRefs = cstTypeDef.findField("method_name_refs");
        final FieldElement methodTypeRefs = cstTypeDef.findField("method_type_refs");
        final Data cst = moduleSection.addData(callSiteTbl, callSiteTbl.getName(), csLiteral);
        cst.setLinkage(Linkage.EXTERNAL);
        cst.setConstant(true);
        final Data cstEnd = moduleSection.addData(callSiteTblSize, callSiteTblSize.getName(), csSizeLiteral);
        cstEnd.setLinkage(Linkage.EXTERNAL);
        cstEnd.setConstant(true);
        final Data sce = moduleSection.addData(sourceTbl, sourceTbl.getName(), sceLiteral);
        sce.setLinkage(Linkage.EXTERNAL);
        sce.setConstant(true);
        final Data se = moduleSection.addData(subprogramTbl, subprogramTbl.getName(), seLiteral);
        se.setLinkage(Linkage.EXTERNAL);
        se.setConstant(true);
        final Data lvi = moduleSection.addData(lviTbl, lviTbl.getName(), lviLiteral);
        lvi.setLinkage(Linkage.EXTERNAL);
        lvi.setConstant(true);
        moduleSection.addData(fileNameRefs, fileNameRefs.getName(), fileNamesLiteral).setLinkage(Linkage.EXTERNAL);
        moduleSection.addData(methodNameRefs, methodNameRefs.getName(), methodNamesLiteral).setLinkage(Linkage.EXTERNAL);
        moduleSection.addData(methodTypeRefs, methodTypeRefs.getName(), methodTypesLiteral).setLinkage(Linkage.EXTERNAL);
    }

    private <T extends VmObject> int lookUpItem(final MutableObjectIntMap<T> table, final List<Literal> literals, final T obj) {
        if (obj == null) {
            // no item
            return -1;
        }
        int val = table.getIfAbsent(obj, -1);
        if (val == -1) {
            val = literals.size();
            table.put(obj, val);
            BuildtimeHeap bh = BuildtimeHeap.get(ctxt);
            literals.add(bh.referToSerializedVmObject(obj, obj.getObjectType().getReference(), ctxt.getOrAddProgramModule(ctxt.getDefaultTypeDefinition())));
        }
        return val;
    }

    private int getSceIndex(StructType sceType, SourceCodeEntry entry, List<Literal> output, MutableObjectIntMap<SourceCodeEntry> scMap, ObjectIntMap<SubprogramEntry> seMap) {
        if (entry == null) {
            return -1;
        }
        int idx = scMap.getIfAbsent(entry, -1);
        if (idx == -1) {
            // construct a literal for it
            Literal entryLit = emitSourceCodeEntry(sceType, entry, seMap::get, sc -> getSceIndex(sceType, sc, output, scMap, seMap));
            scMap.put(entry, idx = output.size());
            output.add(entryLit);
        }
        return idx;
    }

    private static final class TrieNode {
        private final TrieNode successor;
        private final short value;
        private Set<LiveValueInfo> infos;
        private MutableShortObjectMap<TrieNode> predecessors;

        private TrieNode(TrieNode successor, short value) {
            this.successor = successor;
            this.value = value;
        }

        void writeTo(ShortArrayList output, MutableObjectIntMap<LiveValueInfo> offsetMap) {
            if (infos != null) for (LiveValueInfo info : infos) {
                // this LVI can be found at this offset
                offsetMap.getIfAbsentPut(info, output.size());
            }
            output.add(value);
            final TrieNode successor = this.successor;
            if (successor != null) {
                successor.writeTo(output, offsetMap);
            }
        }

        void writeAllTo(ShortArrayList output, MutableObjectIntMap<LiveValueInfo> offsetMap) {
            final MutableShortObjectMap<TrieNode> predecessors = this.predecessors;
            if (predecessors != null) {
                predecessors.forEach(v -> v.writeAllTo(output, offsetMap));
            } else {
                writeTo(output, offsetMap);
            }
        }

        TrieNode findOrAdd(ShortArrayList list, LiveValueInfo info, int idx) {
            if (idx == 0) {
                Set<LiveValueInfo> infos = this.infos;
                if (infos == null) {
                    infos = this.infos = new HashSet<>();
                }
                infos.add(info);
                return this;
            }
            idx--;
            final short iv = list.get(idx);
            MutableShortObjectMap<TrieNode> predecessors = this.predecessors;
            if (predecessors == null) {
                predecessors = this.predecessors = ShortObjectMaps.mutable.empty();
            }
            TrieNode predNode = predecessors.get(iv);
            if (predNode == null) {
                predNode = new TrieNode(this, iv);
                predecessors.put(iv, predNode);
            }
            return predNode.findOrAdd(list, info, idx);
        }
    }

    /**
     * Get the unique subprogram entry object for the given element.
     * Note that this must be called while the interpreter is active.
     *
     * @param executableElement the element (must not be {@code null})
     * @return the unique subprogram entry object (not {@code null})
     */
    public SubprogramEntry getSubprogramEntry(ExecutableElement executableElement) {
        SubprogramEntry entry = subprogramEntries.get(executableElement);
        if (entry == null) {
            final Vm vm = ctxt.getVm();
            String name;
            if (executableElement instanceof NamedElement ne) {
                name = ne.getName();
            } else if (executableElement instanceof ConstructorElement) {
                name = "<init>";
            } else if (executableElement instanceof InitializerElement) {
                // todo: maybe different name for field initializers
                name = "<clinit>";
            } else {
                throw new IllegalArgumentException("Unknown element type");
            }
            final String sourceFileName = executableElement.getSourceFileName();
            VmObject methodType;
            try {
                methodType = vm.createMethodType(executableElement.getEnclosingType().getContext(), executableElement.getDescriptor());
            } catch (IllegalStateException e) {
                ctxt.warning(executableElement.getLocation(), "No method type was created for element");
                methodType = null;
            }
            entry = new SubprogramEntry(
                sourceFileName == null ? null : vm.intern(sourceFileName),
                vm.intern(name),
                methodType,
                executableElement
            );
            final SubprogramEntry appearing = subprogramEntries.putIfAbsent(executableElement, entry);
            if (appearing != null) {
                entry = appearing;
            }
        }
        return entry;
    }

    /**
     * Register the entry list for this element.
     *
     * @param def the type definition which owns the entry (must not be {@code null})
     * @param entries the entry list (must not be {@code null})
     */
    public void registerEntries(LoadedTypeDefinition def, List<CallSiteEntry> entries) {
        Assert.checkNotNullParam("def", def);
        Assert.checkNotNullParam("entries", entries);
        final List<CallSiteEntry> existing = this.entries.putIfAbsent(def, List.copyOf(entries));
        if (existing != null) {
            throw new IllegalArgumentException("Element already registered: " + def);
        }
    }

    /**
     * Get an internalized reference to a value info object.
     *
     * @param valueInfo the value info object (must not be {@code null})
     * @param <V> the value info type
     * @return the internalized value info object, equal to {@code valueInfo} (not {@code null})
     */
    @SuppressWarnings("unchecked")
    public <V extends ValueInfo> V intern(V valueInfo) {
        V existing = (V) valueInfos.putIfAbsent(valueInfo, valueInfo);
        return existing == null ? valueInfo : existing;
    }

    /**
     * Get an internalized reference to a source code entry object.
     *
     * @param sourceCodeEntry the source code entry object (must not be {@code null})
     * @return the internalized source code entry object, equal to {@code sourceCodeEntry} (not {@code null})
     */
    public SourceCodeEntry intern(SourceCodeEntry sourceCodeEntry) {
        SourceCodeEntry existing = sourceCodeEntries.putIfAbsent(sourceCodeEntry, sourceCodeEntry);
        return existing == null ? sourceCodeEntry : existing;
    }

    /**
     * Get an internalized reference to a live value info object which comprises the given set of live values.
     * The given set is copied and so a mutable set can be passed in and subsequently reused.
     *
     * @param liveValues the live value set (must not be {@code null})
     * @return the internalized live value info instance (not {@code null})
     */
    public LiveValueInfo intern(Set<ValueInfo> liveValues) {
        LiveValueInfo interned = liveValueInfos.get(liveValues);
        if (interned != null) {
            return interned;
        }
        final LiveValueInfo newInfo = new LiveValueInfo(Set.copyOf(liveValues));
        interned = liveValueInfos.putIfAbsent(newInfo.liveValues(), newInfo);
        return interned != null ? interned : newInfo;
    }

    public Literal emitCallSiteEntry(StructType callSiteType, CallSiteEntry entry, ToIntFunction<SourceCodeEntry> sceLookup, ToIntFunction<LiveValueInfo> lviLookup) {
        final List<StructType.Member> members = callSiteType.getMembers();
        final StructType.Member ipMember = members.get(0);
        final StructType.Member srcIndexMember = members.get(1);
        final StructType.Member lviIndexMember = members.get(2);
        assert ipMember.getName().equals("ip") && ipMember.getType() instanceof PointerType;
        assert srcIndexMember.getName().equals("source_idx") && srcIndexMember.getType() instanceof UnsignedIntegerType;
        assert lviIndexMember.getName().equals("lvi_idx") && lviIndexMember.getType() instanceof UnsignedIntegerType;
        final LiteralFactory lf = ctxt.getLiteralFactory();
        final UnsignedIntegerType uintptr_t = ipMember.getType(PointerType.class).getSameSizedUnsignedInteger();
        final Function function = entry.fnAddress();
        // add declaration
        ctxt.getOrAddProgramModule(ctxt.getDefaultTypeDefinition()).declareFunction(function);
        final TypeSystem ts = ctxt.getTypeSystem();
        final UnsignedIntegerType u8 = ts.getUnsignedInteger8Type();
        final Literal ipLiteral = lf.offsetFromLiteral(lf.bitcastLiteral(lf.literalOf(function), u8.getPointer()), lf.literalOf(uintptr_t, entry.offset));
        final IntegerLiteral srcIndexLiteral = lf.literalOf(srcIndexMember.getType(IntegerType.class), sceLookup.applyAsInt(entry.sci()));
        final IntegerLiteral lviIndexLiteral = lf.literalOf(lviIndexMember.getType(IntegerType.class), lviLookup.applyAsInt(entry.lvi()));
        return lf.literalOf(callSiteType, Map.of(
            ipMember, ipLiteral,
            srcIndexMember, srcIndexLiteral,
            lviIndexMember, lviIndexLiteral
        ));
    }

    public Literal emitSubprogramEntry(StructType subprogramType, SubprogramEntry entry, ToIntFunction<VmString> fileNameLookup, ToIntFunction<VmString> methodNameLookup, ToIntFunction<VmObject> methodTypeLookup) {
        // strongly dependent on layout, but this allows the structure to be defined in userspace
        final List<StructType.Member> members = subprogramType.getMembers();
        final StructType.Member fileNameMember = members.get(0);
        final StructType.Member methodNameMember = members.get(1);
        final StructType.Member methodTypeMember = members.get(2);
        final StructType.Member typeIdMember = members.get(3);
        final StructType.Member modifiersMember = members.get(4);
        assert fileNameMember.getName().equals("file_name_idx") && fileNameMember.getType() instanceof UnsignedIntegerType;
        assert methodNameMember.getName().equals("method_name_idx") && methodNameMember.getType() instanceof UnsignedIntegerType;
        assert methodTypeMember.getName().equals("method_type_idx") && methodTypeMember.getType() instanceof UnsignedIntegerType;
        assert typeIdMember.getName().equals("type_id") && typeIdMember.getType() instanceof TypeIdType;
        assert modifiersMember.getName().equals("modifiers") && modifiersMember.getType() instanceof IntegerType;
        final LiteralFactory lf = ctxt.getLiteralFactory();
        final IntegerLiteral fileNameIdx = lf.literalOf(fileNameMember.getType(UnsignedIntegerType.class), fileNameLookup.applyAsInt(entry.fileName()));
        final IntegerLiteral methodNameIdx = lf.literalOf(methodNameMember.getType(UnsignedIntegerType.class), methodNameLookup.applyAsInt(entry.name()));
        final IntegerLiteral methodTypeIdx = lf.literalOf(methodTypeMember.getType(UnsignedIntegerType.class), methodTypeLookup.applyAsInt(entry.methodTypeObj()));
        return lf.literalOf(subprogramType, Map.of(
            fileNameMember, fileNameIdx,
            methodNameMember, methodNameIdx,
            methodTypeMember, methodTypeIdx,
            typeIdMember, lf.literalOfType(entry.enclosing().load().getType()),
            modifiersMember, lf.literalOf(modifiersMember.getType(IntegerType.class), entry.modifiers())
        ));
    }

    public Literal emitSourceCodeEntry(StructType sourceCodeType, SourceCodeEntry entry, ToIntFunction<SubprogramEntry> seLookup, ToIntFunction<SourceCodeEntry> sceLookup) {
        final List<StructType.Member> members = sourceCodeType.getMembers();
        final StructType.Member subprogramIdxMember = members.get(0);
        final StructType.Member lineMember = members.get(1);
        final StructType.Member bciMember = members.get(2);
        final StructType.Member inlinedAtSourceIdxMember = members.get(3);
        assert subprogramIdxMember.getName().equals("subprogram_idx") && subprogramIdxMember.getType() instanceof UnsignedIntegerType;
        assert lineMember.getName().equals("line") && lineMember.getType() instanceof UnsignedIntegerType;
        assert bciMember.getName().equals("bci") && bciMember.getType() instanceof UnsignedIntegerType;
        assert inlinedAtSourceIdxMember.getName().equals("inlined_at_source_idx") && inlinedAtSourceIdxMember.getType() instanceof UnsignedIntegerType;
        final LiteralFactory lf = ctxt.getLiteralFactory();
        return lf.literalOf(sourceCodeType, Map.of(
            subprogramIdxMember, lf.literalOf(subprogramIdxMember.getType(IntegerType.class), seLookup.applyAsInt(entry.se())),
            lineMember, lf.literalOf(lineMember.getType(IntegerType.class), entry.line()),
            bciMember, lf.literalOf(bciMember.getType(IntegerType.class), entry.bci()),
            inlinedAtSourceIdxMember, lf.literalOf(inlinedAtSourceIdxMember.getType(IntegerType.class), sceLookup.applyAsInt(entry.inlinedAt()))
        ));
    }

    private static final int LVI_UNSHIFTED = 1 << 15;

    public static short makeLviBaseOffset(int baseReg, int offset) {
        Assert.checkMinimumParameter("baseReg", 0, baseReg);
        Assert.checkMaximumParameter("baseReg", 31, baseReg);
        Assert.checkMinimumParameter("offset", LVI_BO_MIN_VALUE, offset);
        Assert.checkMaximumParameter("offset", LVI_BO_MAX_VALUE, offset);
        return (short) (LVI_UNSHIFTED >>> LVI_BASE_OFFSET | baseReg << 10 | offset & 0b11_1111_1111);
    }

    public static short makeLviInMemory(BitSet bits, int offset) {
        return (short) (LVI_UNSHIFTED >>> LVI_IN_MEMORY | bits.get(offset, offset + 14).toLongArray()[0]);
    }

    public static short makeLviAddOffset(int offset) {
        Assert.checkMinimumParameter("offset", LVI_AO_MIN_VALUE, offset);
        Assert.checkMaximumParameter("offset", LVI_AO_MAX_VALUE, offset);
        return (short) (LVI_UNSHIFTED >>> LVI_ADD_OFFSET | (offset & 0b1_1111_1111_1111));
    }

    public static short makeLviInRegister(int bank, int registers) {
        Assert.checkMinimumParameter("bank", 0, bank);
        Assert.checkMaximumParameter("bank", 3, bank);
        return (short) (LVI_UNSHIFTED >>> LVI_IN_REGISTER | bank << 8 | registers >>> (bank << 3) & 0xff);
    }

    public static short makeLviCurrentAddress() {
        return (short) (LVI_UNSHIFTED >>> LVI_CURRENT_ADDRESS);
    }

    public static short makeLviEndOfList() {
        return (short) 0; // 1 << 16
    }

    public void emitLiveValueInfo(LiveValueInfo entry, ShortArrayList sal) {
        RegisterValueInfo baseRegister = null;
        BitSet memSlots = null;
        IntArrayList stackAllocations = null;
        // the offset of bit 0 in the bit set; always ≤ 0
        int memShift = 0;
        int registers = 0;
        // analyze the live values, which may appear in any order
        for (ValueInfo liveValue : entry.liveValues()) {
            if (liveValue instanceof RegisterValueInfo rvi) {
                registers |= 1 << rvi.getRegisterNumber();
            } else if (liveValue instanceof ConstantValueInfo cvi) {
                if (cvi.getValue().isZero()) {
                    // skip it
                } else {
                    throw new UnsupportedOperationException("Constant reference values");
                }
            } else if (liveValue instanceof FrameOffsetValueInfo fvi) {
                final RegisterValueInfo base = fvi.getBase();
                if (baseRegister == null) {
                    baseRegister = base;
                } else if (baseRegister != base) {
                    // todo: we can support multiple base registers if needed...
                    throw new UnsupportedOperationException("Multiple base registers");
                }
                final int offset = fvi.getOffset();
                if (offset < memShift) {
                    int moreShift = memShift - offset;
                    if (memSlots != null) {
                        // shift it to the left to open more slots at the bottom
                        final int last = memSlots.size() - 1;
                        for (int i = last; i >= 0; i--) {
                            memSlots.set(i + moreShift, memSlots.get(i));
                        }
                        memSlots.clear(0, moreShift);
                    }
                    memShift = offset;
                }
                if (memSlots == null) {
                    memSlots = new BitSet();
                }
                memSlots.set(offset - memShift);
            } else if (liveValue instanceof RegisterRelativeValueInfo rvi) {
                final RegisterValueInfo base = rvi.getBase();
                if (baseRegister == null) {
                    baseRegister = base;
                } else if (baseRegister != base) {
                    // todo: we can support multiple base registers if needed...
                    throw new UnsupportedOperationException("Multiple base registers");
                }
                if (stackAllocations == null) {
                    stackAllocations = new IntArrayList();
                }
                // find insertion point
                final int offset = Math.toIntExact(rvi.getOffset());
                final int ip = stackAllocations.binarySearch(offset);
                if (ip > 0) {
                    // already recorded; ignore
                } else {
                    stackAllocations.addAtIndex(-ip - 1, offset);
                }
            } else {
                throw new UnsupportedOperationException();
            }
        }
        // add the live values to the output
        if ((registers & 0xff) != 0) {
            sal.add(makeLviInRegister(0, registers));
        }
        if ((registers & 0xff_00) != 0) {
            sal.add(makeLviInRegister(1, registers));
        }
        if ((registers & 0xff_00_00) != 0) {
            sal.add(makeLviInRegister(2, registers));
        }
        if ((registers & 0xff_00_00_00) != 0) {
            sal.add(makeLviInRegister(3, registers));
        }
        // memory values; move the starting pointer to at least `-memShift` before moving forward
        if (memSlots != null && !memSlots.isEmpty()) {
            final int regNum = baseRegister.getRegisterNumber();
            // the offset of the next set bit
            int idx = memSlots.nextSetBit(0);
            // the remaining distance from the current address to the address corresponding to bit <<idx>> of the bit set
            int remaining = memShift + idx;
            if (remaining < LVI_BO_MIN_VALUE) {
                // fit within range
                sal.add(makeLviBaseOffset(regNum, LVI_BO_MIN_VALUE));
                remaining -= LVI_BO_MIN_VALUE;
            } else if (remaining <= LVI_BO_MAX_VALUE) {
                // move by a reasonable amount
                sal.add(makeLviBaseOffset(regNum, remaining));
                // we're at the exact location now
                remaining = 0;
            } else {
                // more forward a large amount
                sal.add(makeLviBaseOffset(regNum, LVI_BO_MAX_VALUE));
                remaining -= LVI_BO_MAX_VALUE;
            }
            while (idx != -1) {
                // more locations to process
                if (remaining < LVI_AO_MIN_VALUE) {
                    // move backward a large amount
                    sal.add(makeLviAddOffset(LVI_AO_MIN_VALUE));
                    // this will *increase* offset's value towards zero
                    remaining -= LVI_AO_MIN_VALUE;
                } else if (remaining < 0) {
                    // move backward a reasonable amount
                    sal.add(makeLviAddOffset(remaining));
                    // we're at the exact location now
                    remaining = 0; // offset -= offset;
                } else if (remaining < 14) {
                    // the next bit is in range of an in-memory word
                    // startBit is the location in the bit set that we start emitting
                    final int startBit = idx - remaining;
                    sal.add(makeLviInMemory(memSlots, startBit));
                    // move to next slot(s)
                    idx = memSlots.nextSetBit(startBit + 14);
                    // remaining will contain garbage if idx == -1, but that's OK because the loop will exit
                    remaining = idx - startBit - 14; // remaining = idx - idx(old) + remaining - 14
                } else if (remaining <= LVI_AO_MAX_VALUE) {
                    // move forward a reasonable amount
                    sal.add(makeLviAddOffset(remaining));
                    // we're at the exact location now
                    remaining = 0; // offset -= offset;
                } else {
                    // more forward a large amount
                    sal.add(makeLviAddOffset(LVI_AO_MAX_VALUE));
                    // decrease offset by the distance travelled
                    remaining -= LVI_AO_MAX_VALUE;
                }
            }
        }
        // todo: merge the bitmap and stack-allocated in a clever order
        if (stackAllocations != null && ! stackAllocations.isEmpty()) {
            final int regNum = baseRegister.getRegisterNumber();
            // same base register; just start with the first value
            final int size = stackAllocations.size();
            int offset = stackAllocations.get(0);
            // the remaining distance to traverse
            int remaining = offset;
            if (remaining < LVI_BO_MIN_VALUE) {
                // move backwards by a large amount
                sal.add(makeLviBaseOffset(regNum, LVI_BO_MIN_VALUE));
                remaining -= LVI_BO_MIN_VALUE;
            } else if (remaining <= LVI_BO_MAX_VALUE) {
                // move by a reasonable amount
                sal.add(makeLviBaseOffset(regNum, remaining));
                remaining = 0;
            } else {
                // more forward a large amount
                sal.add(makeLviBaseOffset(regNum, LVI_BO_MAX_VALUE));
                remaining -= LVI_BO_MAX_VALUE;
            }
            while (remaining != 0) {
                // more locations to process
                if (remaining < LVI_AO_MIN_VALUE) {
                    // move backward a large amount
                    sal.add(makeLviAddOffset(LVI_AO_MIN_VALUE));
                    // this will *increase* offset's value towards zero
                    remaining -= LVI_AO_MIN_VALUE;
                } else if (remaining < 0) {
                    // move backward a reasonable amount
                    sal.add(makeLviAddOffset(remaining));
                    // we're at the exact location now
                    remaining = 0; // offset -= offset;
                } else if (remaining <= LVI_AO_MAX_VALUE) {
                    // move forward a reasonable amount
                    sal.add(makeLviAddOffset(remaining));
                    // we're at the exact location now
                    remaining = 0; // offset -= offset;
                } else {
                    // more forward a large amount
                    sal.add(makeLviAddOffset(LVI_AO_MAX_VALUE));
                    // decrease offset by the distance travelled
                    remaining -= LVI_AO_MAX_VALUE;
                }
            }
            sal.add(makeLviCurrentAddress());
            for (int i = 1; i < size; i ++) {
                final int newOffset = stackAllocations.get(i);
                remaining = newOffset - offset;
                offset = newOffset;
                while (remaining != 0) {
                    // more locations to process
                    if (remaining < LVI_AO_MIN_VALUE) {
                        // move backward a large amount
                        sal.add(makeLviAddOffset(LVI_AO_MIN_VALUE));
                        // this will *increase* offset's value towards zero
                        remaining -= LVI_AO_MIN_VALUE;
                    } else if (remaining < 0) {
                        // move backward a reasonable amount
                        sal.add(makeLviAddOffset(remaining));
                        // we're at the exact location now
                        remaining = 0; // offset -= offset;
                    } else if (remaining <= LVI_AO_MAX_VALUE) {
                        // move forward a reasonable amount
                        sal.add(makeLviAddOffset(remaining));
                        // we're at the exact location now
                        remaining = 0; // offset -= offset;
                    } else {
                        // more forward a large amount
                        sal.add(makeLviAddOffset(LVI_AO_MAX_VALUE));
                        // decrease offset by the distance travelled
                        remaining -= LVI_AO_MAX_VALUE;
                    }
                }
                sal.add(makeLviCurrentAddress());
            }
        }
        // end of list
        sal.add(makeLviEndOfList());
    }

    /**
     * A base entry in the instruction table.
     *
     * @param fnAddress the call site function address
     * @param offset the offset into the function of the call site
     * @param sci the source code info for this entry (may be shared)
     * @param lvi the live-value information for this entry (may be shared)
     */
    public record CallSiteEntry(Function fnAddress, long offset, SourceCodeEntry sci, LiveValueInfo lvi) {}

    /**
     * The source code information for a single stack entry.
     *
     * @param se the subprogram entry for this stack entry
     * @param line the source line number
     * @param bci the source bytecode index
     * @param inlinedAt the subprogram entry for the location at which this subprogram was inlined
     */
    public record SourceCodeEntry(SubprogramEntry se, int line, int bci, SourceCodeEntry inlinedAt) {}

    /**
     * Information about each subprogram element.
     *
     * @param fileName the source file name
     * @param name the subprogram name
     * @param methodTypeObj the (reachable) method type object for this entry
     * @param element the element
     */
    public record SubprogramEntry(VmString fileName, VmString name, VmObject methodTypeObj, ExecutableElement element) {
        public SubprogramEntry {
            // validate that there is a type ID early
            element.getEnclosingType().load().getTypeId();
        }

        /**
         * Get the enclosing type definition.
         *
         * @return the enclosing type definition
         */
        public DefinedTypeDefinition enclosing() {
            return element.getEnclosingType();
        }

        /**
         * Get the type ID of the enclosing type definition.
         *
         * @return the type ID
         */
        public int typeId() {
            return enclosing().load().getTypeId();
        }

        /**
         * Get the element modifiers.
         *
         * @return the element modifiers
         */
        public int modifiers() {
            return element.getModifiers();
        }

        /**
         * Get the original method index ID (for sorting purposes only).
         *
         * @return the method index
         */
        public int methodIdx() {
            return element.getIndex();
        }
    }

    /**
     * Information about the live values at a given point, for targets which support live values.
     *
     * @param liveValues the live reference value location set
     */
    public record LiveValueInfo(Set<ValueInfo> liveValues) {}
}

package org.qbicc.plugin.methodinfo;

import org.jboss.logging.Logger;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Driver;
import org.qbicc.graph.Node;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.interpreter.Vm;
import org.qbicc.machine.llvm.stackmap.StackMap;
import org.qbicc.machine.llvm.stackmap.StackMapVisitor;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.object.Data;
import org.qbicc.object.Function;
import org.qbicc.object.ModuleSection;
import org.qbicc.object.ProgramModule;
import org.qbicc.plugin.linker.Linker;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.CompoundType;
import org.qbicc.type.NullableType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * The MethodDataEmitter runs very late in compilation (after the BuildTimeHeap is emitted).
 * All of the Java Strings it needs to reference were already serialized by MethodDataStringsEmitter.
 */
public class MethodDataEmitter implements Consumer<CompilationContext> {
    private static final Logger slog = Logger.getLogger("org.qbicc.plugin.methodinfo.stats");

    // fields for accumulating stats
    private int methodInfoTableCount;
    private int methodInfoTableSize;
    private int sourceCodeInfoTableCount;
    private int sourceCodeInfoTableSize;
    private int sourceCodeIndexListCount;
    private int sourceCodeIndexListSize;
    private int instructionListCount;
    private int instructionListSize;

    private int createMethodInfo(CompilationContext ctxt, NullableType jlsRef, ProgramModule from, MethodData methodData, ExecutableElement element) {
        String methodName = "";
        if (element instanceof ConstructorElement) {
            methodName = "<init>";
        } else if (element instanceof InitializerElement) {
            methodName = "<clinit>";
        } else if (element instanceof MethodElement) {
            methodName = ((MethodElement)element).getName();
        } else if (element instanceof FunctionElement) {
            methodName = ((FunctionElement)element).getName();
        }
        String fileName = element.getSourceFileName();
        String methodDesc = element.getDescriptor().toString();
        int typeId = element.getEnclosingType().load().getTypeId();
        if (typeId == -1) {
            // todo: we should use 0 always as the invalid type ID value
            typeId = 0;
        }

        Vm vm = ctxt.getVm();
        BuildtimeHeap btHeap = BuildtimeHeap.get(ctxt);
        Literal fnLiteral;
        if (fileName == null) {
            fnLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(jlsRef);
        } else {
            fnLiteral = btHeap.referToSerializedVmObject(vm.intern(fileName), jlsRef, from);
        }
        Literal mnLiteral = btHeap.referToSerializedVmObject(vm.intern(methodName), jlsRef, from);
        Literal mdLiteral = btHeap.referToSerializedVmObject(vm.intern(methodDesc), jlsRef, from);

        return methodData.add(new MethodInfo(fnLiteral, mnLiteral, mdLiteral, typeId, element.getModifiers()));
    }

    private int createSourceCodeInfo(CompilationContext ctxt, MethodData methodData,  NullableType jlsRef, ProgramModule from, ExecutableElement element, int lineNumber, int bcIndex, int inlinedAtIndex) {
        int minfoIndex = createMethodInfo(ctxt, jlsRef, from, methodData, element);
        return methodData.add(new SourceCodeInfo(minfoIndex, lineNumber, bcIndex, inlinedAtIndex));
    }

    private int createSourceCodeInfo(CompilationContext ctxt, MethodData methodData,  NullableType jlsRef, ProgramModule from, Node node) {
        int sourceCodeIndex;
        ExecutableElement element = node.getElement();
        if (node.getCallSite() == null) {
            sourceCodeIndex = createSourceCodeInfo(ctxt, methodData, jlsRef, from, element, node.getSourceLine(), node.getBytecodeIndex(), -1);
        } else {
            int callerIndex = createSourceCodeInfo(ctxt, methodData, jlsRef, from, node.getCallSite());
            sourceCodeIndex = createSourceCodeInfo(ctxt, methodData, jlsRef, from, element, node.getSourceLine(), node.getBytecodeIndex(), callerIndex);
        }
        return sourceCodeIndex;
    }

    /**
     * Returns root method of the sequence of inlined methods starting from the
     * method which contains {@code node}.
     *
     * @param node
     * @return root method of the inlined method sequence
     */
    private ExecutableElement getRootMethodOfInlineSequence(Node node) {
        if (node.getCallSite() == null) {
            return node.getElement();
        } else {
            return getRootMethodOfInlineSequence(node.getCallSite());
        }
    }

    public MethodData createMethodData(CompilationContext ctxt) {
        List<StackMapRecord> stackMapRecords = new StackMapRecordCollector(ctxt).collect();
        CallSiteInfo callSiteInfo = ctxt.getAttachment(CallSiteInfo.KEY);
        if (stackMapRecords.isEmpty()) {
            return new MethodData(0);
        }
        MethodData methodData = new MethodData(stackMapRecords.size());
        Iterator<StackMapRecord> recordIterator = stackMapRecords.iterator();
        final int[] recordIndex = { 0 };
        ReferenceType jlsRef = ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load().getObjectType().getReference();
        ProgramModule module = ctxt.getOrAddProgramModule(ctxt.getDefaultTypeDefinition());

        ctxt.runParallelTask(context -> {
            StackMapRecord record;
            int index;
            for (;;) {
                synchronized (recordIterator) {
                    if (!recordIterator.hasNext()) {
                        return;
                    }
                    record = recordIterator.next();
                    index = recordIndex[0];
                    recordIndex[0] += 1;
                }

                long spId = record.getStatepoindId();
                int instructionOffset = record.getOffset();
                Node node = callSiteInfo.getNodeForStatepointId((int)spId);
                int scIndex = createSourceCodeInfo(ctxt, methodData, jlsRef, module, node);
                methodData.add(index, new InstructionMap(instructionOffset, scIndex, getRootMethodOfInlineSequence(node)));
            }
        });

        return methodData;
    }

    private Data defineData(CompilationContext ctxt, String variableName, Literal value) {
        ModuleSection section = ctxt.getImplicitSection(ctxt.getDefaultTypeDefinition());
        return section.addData(null, variableName, value);
    }

    Literal emitMethodInfoTable(CompilationContext ctxt, MethodInfo[] minfoList) {
        TypeSystem ts = ctxt.getTypeSystem();
        LiteralFactory lf = ctxt.getLiteralFactory();
        MethodDataTypes mdTypes = MethodDataTypes.get(ctxt);

        LoadedTypeDefinition jls = ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load();
        ReferenceType jlsRef = jls.getObjectType().getReference();
        CompoundType methodInfoType = mdTypes.getMethodInfoType();

        Literal[] minfoLiterals = Arrays.stream(minfoList).parallel().map(minfo -> {
            HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
            Literal fnLiteral = minfo.getFileNameSymbolLiteral();
            Literal mnLiteral = minfo.getMethodNameSymbolLiteral();
            Literal mdLiteral = minfo.getMethodDescSymbolLiteral();
            Literal typeIdLiteral = lf.literalOf(minfo.getTypeId());
            Literal modifiersLiteral = lf.literalOf(minfo.getModifiers());

            valueMap.put(methodInfoType.getMember("fileName"), fnLiteral);
            valueMap.put(methodInfoType.getMember("methodName"), mnLiteral);
            valueMap.put(methodInfoType.getMember("methodDesc"), mdLiteral);
            valueMap.put(methodInfoType.getMember("typeId"), typeIdLiteral);
            valueMap.put(methodInfoType.getMember("modifiers"), modifiersLiteral);
            return lf.literalOf(methodInfoType, valueMap);
        }).toArray(Literal[]::new);

        methodInfoTableCount += minfoLiterals.length;
        methodInfoTableSize += methodInfoTableCount * methodInfoType.getSize();

        Data data = defineData(ctxt, "qbicc_method_info_table", lf.literalOf(ts.getArrayType(methodInfoType, minfoLiterals.length), List.of(minfoLiterals)));
        return lf.literalOf(data);
    }

    Literal emitSourceCodeInfoTable(CompilationContext ctxt, SourceCodeInfo[] scList) {
        TypeSystem ts = ctxt.getTypeSystem();
        LiteralFactory lf = ctxt.getLiteralFactory();
        MethodDataTypes mdTypes = MethodDataTypes.get(ctxt);

        CompoundType sourceCodeInfoType = mdTypes.getSourceCodeInfoType();

        Literal[] scInfoLiterals = Arrays.stream(scList).parallel().map(scInfo -> {
            HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
            valueMap.put(sourceCodeInfoType.getMember("methodInfoIndex"), lf.literalOf(scInfo.getMethodInfoIndex()));
            valueMap.put(sourceCodeInfoType.getMember("lineNumber"), lf.literalOf(scInfo.getLineNumber()));
            valueMap.put(sourceCodeInfoType.getMember("bcIndex"), lf.literalOf(scInfo.getBcIndex()));
            valueMap.put(sourceCodeInfoType.getMember("inlinedAtIndex"), lf.literalOf(scInfo.getInlinedAtIndex()));
            return lf.literalOf(sourceCodeInfoType, valueMap);
        }).toArray(Literal[]::new);

        sourceCodeInfoTableCount += scInfoLiterals.length;
        sourceCodeInfoTableSize += sourceCodeInfoTableCount * sourceCodeInfoType.getSize();

        Data data = defineData(ctxt, "qbicc_source_code_info_table", lf.literalOf(ts.getArrayType(sourceCodeInfoType, scInfoLiterals.length), List.of(scInfoLiterals)));
        return lf.literalOf(data);
    }

    Literal emitSourceCodeIndexList(CompilationContext ctxt, InstructionMap[] imapList) {
        TypeSystem ts = ctxt.getTypeSystem();
        LiteralFactory lf = ctxt.getLiteralFactory();
        ValueType uint32Type = ts.getUnsignedInteger32Type();

        Literal[] scIndexLiterals = IntStream.range(0, imapList.length)
            .parallel()
            .mapToObj(i -> lf.literalOf(imapList[i].getSourceCodeIndex())).toArray(Literal[]::new);

        sourceCodeIndexListCount += scIndexLiterals.length;
        sourceCodeIndexListSize += sourceCodeIndexListCount * uint32Type.getSize();

        Data data = defineData(ctxt, "qbicc_source_code_index_table", lf.literalOf(ts.getArrayType(uint32Type, scIndexLiterals.length), List.of(scIndexLiterals)));
        return lf.literalOf(data);
    }

    Literal emitInstructionList(CompilationContext ctxt, InstructionMap[] imapList) {
        TypeSystem ts = ctxt.getTypeSystem();
        LiteralFactory lf = ctxt.getLiteralFactory();
        ProgramModule programModule = ctxt.getOrAddProgramModule(ctxt.getDefaultTypeDefinition());
        ValueType uint64Type = ts.getUnsignedInteger64Type();

        Literal[] instructionLiterals = IntStream.range(0, imapList.length)
            .parallel()
            .mapToObj(i -> {
                Function function = ctxt.getExactFunction(imapList[i].getFunction());
                Literal functionCastLiteral = lf.bitcastLiteral(lf.literalOf(function), ts.getUnsignedInteger8Type().getPointer());
                Literal instructionAddrLiteral = lf.valueConvertLiteral(lf.elementOfLiteral(functionCastLiteral, lf.literalOf(imapList[i].getOffset())), ts.getUnsignedInteger64Type());
                programModule.declareFunction(null, function.getName(), function.getValueType());
                return instructionAddrLiteral;
            }).toArray(Literal[]::new);

        instructionListCount += instructionLiterals.length;
        instructionListSize += instructionListCount * uint64Type.getSize();

        Data data = defineData(ctxt, "qbicc_instruction_table", lf.literalOf(ts.getArrayType(uint64Type, instructionLiterals.length), List.of(instructionLiterals)));
        return lf.literalOf(data);
    }

    void emitGlobalMethodData(CompilationContext ctxt,
                              ProgramObjectLiteral minfoTable,
                              ProgramObjectLiteral scInfoTable,
                              ProgramObjectLiteral scIndexTable,
                              ProgramObjectLiteral instructionTable,
                              int instructionTableSize) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        TypeSystem ts = ctxt.getTypeSystem();
        MethodDataTypes mdTypes = MethodDataTypes.get(ctxt);
        CompoundType mdhType = mdTypes.getGlobalMethodDataType();
        HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
        CompoundType.Member member;

        member = mdhType.getMember("methodInfoTable");
        valueMap.put(member, lf.bitcastLiteral(minfoTable, (WordType) member.getType()));

        member = mdhType.getMember("sourceCodeInfoTable");
        valueMap.put(member, lf.bitcastLiteral(scInfoTable, (WordType) member.getType()));

        member = mdhType.getMember("sourceCodeIndexTable");
        valueMap.put(member, lf.bitcastLiteral(scIndexTable, (WordType) member.getType()));

        member = mdhType.getMember("instructionTable");
        valueMap.put(member, lf.bitcastLiteral(instructionTable, (WordType) member.getType()));

        valueMap.put(mdhType.getMember("instructionTableSize"), lf.literalOf(instructionTableSize));

        Literal mdhLiteral = lf.literalOf(mdhType, valueMap);
        defineData(ctxt, MethodDataTypes.QBICC_GLOBAL_METHOD_DATA, mdhLiteral);
    }

    public void emitMethodData(CompilationContext ctxt, MethodData methodData) {
        ProgramObjectLiteral minfoTableSymbol = (ProgramObjectLiteral) emitMethodInfoTable(ctxt, methodData.getMethodInfoTable());
        ProgramObjectLiteral scInfoTableSymbol = (ProgramObjectLiteral) emitSourceCodeInfoTable(ctxt, methodData.getSourceCodeInfoTable());
        ProgramObjectLiteral scIndexTableSymbol = (ProgramObjectLiteral) emitSourceCodeIndexList(ctxt, methodData.getInstructionMapList());
        ProgramObjectLiteral instructionTableSymbol = (ProgramObjectLiteral) emitInstructionList(ctxt, methodData.getInstructionMapList());
        emitGlobalMethodData(ctxt, minfoTableSymbol, scInfoTableSymbol, scIndexTableSymbol, instructionTableSymbol, methodData.getInstructionMapList().length);
    }

    private void displayStats() {
        slog.debug("Method Data stats");
        slog.debug("-----------------");
        slog.debugf("qbicc_method_info_table entry count: %d", methodInfoTableCount);
        slog.debugf("qbicc_method_info_table size: %d bytes", methodInfoTableSize);
        slog.debugf("qbicc_source_code_info_table entry count: %d", sourceCodeInfoTableCount);
        slog.debugf("qbicc_source_code_info_table size: %d bytes", sourceCodeInfoTableSize);
        slog.debugf("qbicc_source_code_index_list entry count: %d", sourceCodeIndexListCount);
        slog.debugf("qbicc_source_code_index_list size: %d bytes", sourceCodeIndexListSize);
        slog.debugf("qbicc_instruction_list entry count: %d", instructionListCount);
        slog.debugf("qbicc_instruction_list size: %d bytes", instructionListSize);
    }

    @Override
    public void accept(CompilationContext context) {
        MethodData methodData = createMethodData(context);
        emitMethodData(context, methodData);
        displayStats();
    }

    private static class StackMapRecord implements Comparable {
        private final int objectFileIndex;
        private final int functionIndex;
        private final int offset;
        private final long statepoindId;

        StackMapRecord(final int objectFileIndex, final int functionIndex, final int offset, final long statepoindId) {
            this.objectFileIndex = objectFileIndex;
            this.functionIndex = functionIndex;
            this.offset = offset;
            this.statepoindId = statepoindId;
        }

        long getStatepoindId() { return statepoindId; }
        int getOffset() { return offset; }

        @Override
        public int hashCode() {
            return Objects.hash(objectFileIndex, statepoindId, offset);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            return equals((StackMapRecord)o);
        }

        public boolean equals(StackMapRecord record) {
            return objectFileIndex == record.objectFileIndex
                && offset == record.offset;
        }

        @Override
        public int compareTo(Object o) {
            StackMapRecord other = (StackMapRecord) o;
            if (equals(other)) {
                return 0;
            }
            int diff = Integer.compareUnsigned(objectFileIndex, other.objectFileIndex);
            if (diff == 0) {
                diff = Integer.compareUnsigned(functionIndex, other.functionIndex);
            }
            if (diff == 0) {
                diff = Integer.compareUnsigned(offset, other.offset);
            }
            return diff;
        }
    }

    private final class StackMapRecordCollector {
        CompilationContext context;
        StackMapRecordCollector(CompilationContext context) {
            this.context = context;
        }

        public List<StackMapRecord> collect() {
            Linker linker = Linker.get(context);
            List<StackMapRecord> recordList = new ArrayList<>();
            ObjectFileProvider objFileProvider = context.getAttachment(Driver.OBJ_PROVIDER_TOOL_KEY);
            Iterator<Path> objFileIterator = linker.getObjectFilePathsByType().values().iterator();
            final int[] index = { 0 };

            context.runParallelTask(ctxt -> {
                Path objFile;
                for (;;) {
                    int objFileIndex;
                    synchronized (objFileIterator) {
                        if (!objFileIterator.hasNext()) {
                            return;
                        }
                        objFile = objFileIterator.next();
                        objFileIndex = index[0];
                        index[0] += 1;
                    }
                    try (ObjectFile objectFile = objFileProvider.openObjectFile(objFile)) {
                        org.qbicc.machine.object.Section stackMapSection = objectFile.getSection(objectFile.getStackMapSectionName());
                        if (stackMapSection != null) {
                            ByteBuffer stackMapData = stackMapSection.getSectionContent();
                            StackMap.parse(stackMapData, new StackMapVisitor() {
                                private long currentFnIndex;
                                public void startFunction(long fnIndex, long address, long stackSize, long recordCount) {
                                    currentFnIndex = fnIndex;
                                }
                                public void startRecord(long recIndex, long patchPointId, long offset, int locCnt, int liveOutCnt) {
                                    synchronized (recordList) {
                                        recordList.add(new StackMapRecord(objFileIndex, (int)currentFnIndex, (int) offset, patchPointId));
                                    }
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            // sort the list based on the object file index, function index and the instruction offset
            recordList.sort(StackMapRecord::compareTo);
            return recordList;
        }
    }
}

package org.qbicc.plugin.methodinfo;

import org.jboss.logging.Logger;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Driver;
import org.qbicc.graph.Node;
import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.machine.llvm.stackmap.StackMap;
import org.qbicc.machine.llvm.stackmap.StackMapVisitor;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.object.Function;
import org.qbicc.object.Section;
import org.qbicc.plugin.linker.Linker;
import org.qbicc.plugin.llvm.LLVMCallSiteInfo;
import org.qbicc.plugin.stringpool.StringId;
import org.qbicc.plugin.stringpool.StringPool;
import org.qbicc.type.CompoundType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
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

    private int createMethodInfo(CompilationContext ctxt, MethodData methodData, ExecutableElement element) {
        StringPool stringPool = StringPool.get(ctxt);
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
        String className = element.getEnclosingType().getInternalName();
        String methodDesc = element.getDescriptor().toString();
        StringId fileNameIndex = stringPool.add(fileName);
        StringId classNameIndex = stringPool.add(className);
        StringId methodNameIndex = stringPool.add(methodName);
        StringId methodDescIndex = stringPool.add(methodDesc);
        return methodData.add(new MethodInfo(fileNameIndex, classNameIndex, methodNameIndex, methodDescIndex));
    }

    private int createSourceCodeInfo(CompilationContext ctxt, MethodData methodData, ExecutableElement element, int lineNumber, int bcIndex, int inlinedAtIndex) {
        int minfoIndex = createMethodInfo(ctxt, methodData, element);
        return methodData.add(new SourceCodeInfo(minfoIndex, lineNumber, bcIndex, inlinedAtIndex));
    }

    private int createSourceCodeInfo(CompilationContext ctxt, MethodData methodData, Node node) {
        int sourceCodeIndex;
        ExecutableElement element = node.getElement();
        if (node.getCallSite() == null) {
            sourceCodeIndex = createSourceCodeInfo(ctxt, methodData, element, node.getSourceLine(), node.getBytecodeIndex(), -1);
        } else {
            int callerIndex = createSourceCodeInfo(ctxt, methodData, node.getCallSite());
            sourceCodeIndex = createSourceCodeInfo(ctxt, methodData, element, node.getSourceLine(), node.getBytecodeIndex(), callerIndex);
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
        LLVMCallSiteInfo callSiteInfo = ctxt.getAttachment(LLVMCallSiteInfo.KEY);
        MethodData methodData = new MethodData(stackMapRecords.size());
        Iterator<StackMapRecord> recordIterator = stackMapRecords.iterator();
        final int[] recordIndex = { 0 };

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
                int scIndex = createSourceCodeInfo(ctxt, methodData, node);
                methodData.add(index, new InstructionMap(instructionOffset, scIndex, getRootMethodOfInlineSequence(node)));
            }
        });

        return methodData;
    }

    Literal emitMethodInfoTable(CompilationContext ctxt, MethodInfo[] minfoList) {
        TypeSystem ts = ctxt.getTypeSystem();
        LiteralFactory lf = ctxt.getLiteralFactory();
        ValueType uint32Type = ts.getUnsignedInteger32Type();
        CompoundType methodInfoType = CompoundType.builder(ts)
            .setTag(CompoundType.Tag.STRUCT)
            .setName("qbicc_method_info")
            .setOverallAlignment(uint32Type.getAlign())
            .addNextMember("fileNameIndex", uint32Type)
            .addNextMember("classNameIndex", uint32Type)
            .addNextMember("methodNameIndex", uint32Type)
            .addNextMember("methodDescIndex", uint32Type)
            .build();

        Literal[] minfoLiterals = Arrays.stream(minfoList).parallel().map(minfo -> {
                HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
                valueMap.put(methodInfoType.getMember(0), minfo.getFileNameStringId().serialize(ctxt));
                valueMap.put(methodInfoType.getMember(1), minfo.getClassNameStringId().serialize(ctxt));
                valueMap.put(methodInfoType.getMember(2), minfo.getMethodNameStringId().serialize(ctxt));
                valueMap.put(methodInfoType.getMember(3), minfo.getMethodDescStringId().serialize(ctxt));
                return lf.literalOf(methodInfoType, valueMap);
        }).toArray(Literal[]::new);

        methodInfoTableCount += minfoLiterals.length;
        methodInfoTableSize += methodInfoTableCount * methodInfoType.getSize();

        return lf.literalOf(ts.getArrayType(methodInfoType, minfoLiterals.length), List.of(minfoLiterals));
    }

    Literal emitSourceCodeInfoTable(CompilationContext ctxt, SourceCodeInfo[] scList) {
        TypeSystem ts = ctxt.getTypeSystem();
        LiteralFactory lf = ctxt.getLiteralFactory();
        ValueType uint32Type = ts.getUnsignedInteger32Type();
        CompoundType sourceCodeInfoType = CompoundType.builder(ts)
            .setTag(CompoundType.Tag.STRUCT)
            .setName("qbicc_souce_code_info")
            .setOverallAlignment(uint32Type.getAlign())
            .addNextMember("methodInfoIndex", uint32Type)
            .addNextMember("lineNumber", uint32Type)
            .addNextMember("bcIndex", uint32Type)
            .addNextMember("inlinedAtIndex", uint32Type)
            .build();

        Literal[] scInfoLiterals = Arrays.stream(scList).parallel().map(scInfo -> {
            HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
            valueMap.put(sourceCodeInfoType.getMember(0), lf.literalOf(scInfo.getMethodInfoIndex()));
            valueMap.put(sourceCodeInfoType.getMember(1), lf.literalOf(scInfo.getLineNumber()));
            valueMap.put(sourceCodeInfoType.getMember(2), lf.literalOf(scInfo.getBcIndex()));
            valueMap.put(sourceCodeInfoType.getMember(3), lf.literalOf(scInfo.getInlinedAtIndex()));
            return lf.literalOf(sourceCodeInfoType, valueMap);
        }).toArray(Literal[]::new);

        sourceCodeInfoTableCount += scInfoLiterals.length;
        sourceCodeInfoTableSize += sourceCodeInfoTableCount * sourceCodeInfoType.getSize();

        return lf.literalOf(ts.getArrayType(sourceCodeInfoType, scInfoLiterals.length), List.of(scInfoLiterals));
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

        return lf.literalOf(ts.getArrayType(uint32Type, scIndexLiterals.length), List.of(scIndexLiterals));
    }

    Literal emitInstructionList(CompilationContext ctxt, InstructionMap[] imapList) {
        TypeSystem ts = ctxt.getTypeSystem();
        LiteralFactory lf = ctxt.getLiteralFactory();
        Section section = ctxt.getImplicitSection(ctxt.getDefaultTypeDefinition());
        ValueType uint64Type = ts.getUnsignedInteger64Type();

        Literal[] instructionLiterals = IntStream.range(0, imapList.length)
            .parallel()
            .mapToObj(i -> {
                Function function = ctxt.getExactFunction(imapList[i].getFunction());
                Literal functionCastLiteral = lf.bitcastLiteral(lf.literalOfSymbol(function.getName(), function.getType().getPointer()), ts.getUnsignedInteger8Type().getPointer());
                Literal instructionAddrLiteral = lf.valueConvertLiteral(lf.elementOfLiteral(functionCastLiteral, lf.literalOf(imapList[i].getOffset())), ts.getUnsignedInteger64Type());
                section.declareFunction(null, function.getName(), function.getType());
                return instructionAddrLiteral;
            }).toArray(Literal[]::new);

        instructionListCount += instructionLiterals.length;
        instructionListSize += instructionListCount * uint64Type.getSize();

        return lf.literalOf(ts.getArrayType(uint64Type, instructionLiterals.length), List.of(instructionLiterals));
    }

    Literal emitInstructionListCount(CompilationContext ctxt, InstructionMap[] imapList) {
        return ctxt.getLiteralFactory().literalOf(imapList.length);
    }

    private void emitGlobalVariable(CompilationContext ctxt, String variableName, Literal value) {
        Section section = ctxt.getImplicitSection(ctxt.getDefaultTypeDefinition());
        section.addData(null, variableName, value);
    }

    public void emitMethodData(CompilationContext ctxt, MethodData methodData) {
        Literal value;

        value = emitMethodInfoTable(ctxt, methodData.getMethodInfoTable());
        emitGlobalVariable(ctxt, "qbicc_method_info_table", value);

        value = emitSourceCodeInfoTable(ctxt, methodData.getSourceCodeInfoTable());
        emitGlobalVariable(ctxt, "qbicc_source_code_info_table", value);

        value = emitInstructionList(ctxt, methodData.getInstructionMapList());
        emitGlobalVariable(ctxt, "qbicc_instruction_list", value);

        value = emitInstructionListCount(ctxt, methodData.getInstructionMapList());
        emitGlobalVariable(ctxt, "qbicc_instruction_list_size", value);

        value = emitSourceCodeIndexList(ctxt, methodData.getInstructionMapList());
        emitGlobalVariable(ctxt, "qbicc_source_code_index_list", value);
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
        private final int offset;
        private final long statepoindId;

        StackMapRecord(final int objectFileIndex, final int offset, final long statepoindId) {
            this.objectFileIndex = objectFileIndex;
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
            if (objectFileIndex < other.objectFileIndex) {
                return -1;
            } else if (objectFileIndex > other.objectFileIndex) {
                return 1;
            } else if (offset < other.offset) { // if same objectFileIndex then order by offset
                return -1;
            } else {
                return 1;
            }
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
            Iterator<Path> objFileIterator = linker.getObjectFilePaths().iterator();
            final Integer[] index = { 0 };

            context.runParallelTask(ctxt -> {
                Path objFile;
                for (;;) {
                    Integer objFileIndex;
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
                                public void startRecord(long recIndex, long patchPointId, long offset, int locCnt, int liveOutCnt) {
                                    synchronized (recordList) {
                                        recordList.add(new StackMapRecord(objFileIndex, (int) offset, patchPointId));
                                    }
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            // sort the list based on the object file index and the instruction offset
            recordList.sort(StackMapRecord::compareTo);
            return recordList;
        }
    }
}

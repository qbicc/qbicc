package org.qbicc.plugin.methodinfo;

import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Driver;
import org.qbicc.graph.Node;
import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.machine.llvm.stackmap.StackMap;
import org.qbicc.machine.llvm.stackmap.StackMapVisitor;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.object.Function;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.Section;
import org.qbicc.plugin.linker.Linker;
import org.qbicc.plugin.llvm.LLVMCallSiteInfo;
import org.qbicc.plugin.llvm.LLVMCompiler;
import org.qbicc.plugin.llvm.LLVMGenerator;
import org.qbicc.type.CompoundType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.generic.BaseTypeSignature;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class MethodDataEmitter implements Consumer<CompilationContext> {
    private final boolean isPie;
    private final AtomicInteger stringIndex = new AtomicInteger();

    private int createMethodInfo(MethodData methodData, ExecutableElement element) {
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
        int fileNameIndex = fileName != null ? methodData.addFileName(fileName) : -1;
        int classNameIndex = className != null ? methodData.addClassName(className) : -1;
        int methodNameIndex = methodName != null ? methodData.addMethodName(methodName) : -1;
        int methodDescIndex = methodDesc != null ? methodData.addMethodDesc(methodDesc) : -1;
        return methodData.add(new MethodInfo(fileNameIndex, classNameIndex, methodNameIndex, methodDescIndex));
    }

    private int createSourceCodeInfo(MethodData methodData, ExecutableElement element, int lineNumber, int bcIndex, int inlinedAtIndex) {
        int minfoIndex = createMethodInfo(methodData, element);
        return methodData.add(new SourceCodeInfo(minfoIndex, lineNumber, bcIndex, inlinedAtIndex));
    }

    private int createSourceCodeInfo(MethodData methodData, Node node) {
        int sourceCodeIndex;
        ExecutableElement element = node.getElement();
        if (node.getCallSite() == null) {
            sourceCodeIndex = createSourceCodeInfo(methodData, element, node.getSourceLine(), node.getBytecodeIndex(), -1);
        } else {
            int callerIndex = createSourceCodeInfo(methodData, node.getCallSite());
            sourceCodeIndex = createSourceCodeInfo(methodData, element, node.getSourceLine(), node.getBytecodeIndex(), callerIndex);
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
                int scIndex = createSourceCodeInfo(methodData, node);
                methodData.add(index, new InstructionMap(instructionOffset, scIndex, getRootMethodOfInlineSequence(node)));
            }
        });

        return methodData;
    }

    Literal emitStringTable(CompilationContext ctxt, String[] strings) {
        TypeSystem ts = ctxt.getTypeSystem();
        LiteralFactory lf = ctxt.getLiteralFactory();
        Section section = ctxt.getImplicitSection(ctxt.getDefaultTypeDefinition());

        // strings are represented using a compound type consisting of length and a pointer to character data
        CompoundType stringType = CompoundType.builder(ts)
            .setTag(CompoundType.Tag.STRUCT)
            .setName("qbicc-md-string")
            .setOverallAlignment(ts.getUnsignedInteger32Type().getAlign())
            .addNextMember("len", ts.getUnsignedInteger32Type())
            .addNextMember("data", ts.getUnsignedInteger8Type().getPointer())
            .build();

        Literal[] literals = Arrays.stream(strings)
            .parallel()
            .map(str -> {
                // create a literal for the array of characters
                byte[] chars = str.getBytes();
                Literal literal = lf.literalOf(ts.getArrayType(ts.getUnsignedInteger8Type(), chars.length), chars);
                SymbolLiteral symbol = lf.literalOfSymbol(".qbicc-md-string-data-" + stringIndex.getAndIncrement(), literal.getType().getPointer());
                section.addData(null, symbol.getName(), literal);
                Literal ptrLiteral = lf.bitcastLiteral(symbol, ts.getUnsignedInteger8Type().getPointer());

                // now create a literal for the string consisting of length and the pointer to char array
                HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
                valueMap.put(stringType.getMember(0), lf.literalOf(chars.length));
                valueMap.put(stringType.getMember(1), ptrLiteral);
                return lf.literalOf(stringType, valueMap);
            }).toArray(Literal[]::new);

        return lf.literalOf(ts.getArrayType(literals[0].getType(), literals.length), List.of(literals));
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
                valueMap.put(methodInfoType.getMember(0), lf.literalOf(minfo.getFileNameIndex()));
                valueMap.put(methodInfoType.getMember(1), lf.literalOf(minfo.getClassNameIndex()));
                valueMap.put(methodInfoType.getMember(2), lf.literalOf(minfo.getMethodNameIndex()));
                valueMap.put(methodInfoType.getMember(3), lf.literalOf(minfo.getMethodDescIndex()));
                return lf.literalOf(methodInfoType, valueMap);
        }).toArray(Literal[]::new);

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

        return lf.literalOf(ts.getArrayType(sourceCodeInfoType, scInfoLiterals.length), List.of(scInfoLiterals));
    }

    Literal emitInstructionMap(CompilationContext ctxt, InstructionMap[] imapList) {
        TypeSystem ts = ctxt.getTypeSystem();
        LiteralFactory lf = ctxt.getLiteralFactory();
        Section section = ctxt.getImplicitSection(ctxt.getDefaultTypeDefinition());
        ValueType uint64Type = ts.getUnsignedInteger64Type();
        ValueType uint32Type = ts.getUnsignedInteger32Type();

        CompoundType imapType = CompoundType.builder(ts)
            .setTag(CompoundType.Tag.STRUCT)
            .setName("qbicc_instruction_map")
            .setOverallAlignment(uint64Type.getAlign())
            .addNextMember("instructionAddress", uint64Type)
            .addNextMember("sourceCodeIndex", uint32Type)
            .build();

        Literal[] imapLiterals = IntStream.range(0, imapList.length)
            .parallel()
            .mapToObj(i -> {
            HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
            Function function = ctxt.getExactFunction(imapList[i].getFunction());
            Literal functionCastLiteral = lf.bitcastLiteral(lf.literalOfSymbol(function.getName(), function.getType().getPointer()), ts.getUnsignedInteger8Type().getPointer());
            Literal instructionAddrLiteral = lf.valueConvertLiteral(lf.elementOfLiteral(functionCastLiteral, lf.literalOf(imapList[i].getOffset())), ts.getUnsignedInteger64Type());
            valueMap.put(imapType.getMember(0), instructionAddrLiteral);
            valueMap.put(imapType.getMember(1), lf.literalOf(imapList[i].getSourceCodeIndex()));
            section.declareFunction(null, function.getName(), function.getType());
            return lf.literalOf(imapType, valueMap);
        }).toArray(Literal[]::new);

        return lf.literalOf(ts.getArrayType(imapType, imapLiterals.length), List.of(imapLiterals));
    }

    private void emitGlobalVariable(CompilationContext ctxt, String variableName, Literal value) {
        Section section = ctxt.getImplicitSection(ctxt.getDefaultTypeDefinition());
        section.addData(null, variableName, value);
    }

    public Path emitMethodData(CompilationContext ctxt, MethodData methodData) {
        ArrayLiteral value;

        value = (ArrayLiteral) emitStringTable(ctxt, methodData.getFileNameList());
        emitGlobalVariable(ctxt, "qbicc_filename_table", value);

        value = (ArrayLiteral) emitStringTable(ctxt, methodData.getClassNameList());
        emitGlobalVariable(ctxt, "qbicc_classname_table", value);

        value = (ArrayLiteral) emitStringTable(ctxt, methodData.getMethodNameList());
        emitGlobalVariable(ctxt, "qbicc_methodname_table", value);

        value = (ArrayLiteral) emitStringTable(ctxt, methodData.getMethodDescList());
        emitGlobalVariable(ctxt, "qbicc_methoddesc_table", value);

        value = (ArrayLiteral) emitMethodInfoTable(ctxt, methodData.getMethodInfoTable());
        emitGlobalVariable(ctxt, "qbicc_method_info_table", value);

        value = (ArrayLiteral) emitSourceCodeInfoTable(ctxt, methodData.getSourceCodeInfoTable());
        emitGlobalVariable(ctxt, "qbicc_source_code_info_table", value);

        value = (ArrayLiteral) emitInstructionMap(ctxt, methodData.getInstructionMapList());
        emitGlobalVariable(ctxt, "qbicc_instruction_map_table", value);

        ProgramModule programModule = ctxt.getProgramModule(ctxt.getDefaultTypeDefinition());
        return new LLVMGenerator(isPie ? 2 : 0, isPie ? 2 : 0).processProgramModule(ctxt, programModule);
    }

    public MethodDataEmitter(boolean isPie) {
        this.isPie = isPie;
    }

    private void compileMethodDataModule(CompilationContext context, Path modulePath) {
        LLVMCompileStage compileStage = new LLVMCompileStage(isPie);
        compileStage.compileModule(context, modulePath);
    }

    @Override
    public void accept(CompilationContext context) {
        MethodData methodData = createMethodData(context);
        Path modulePath = emitMethodData(context, methodData);
        if (modulePath != null) {
            compileMethodDataModule(context, modulePath);
        }
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
                        org.qbicc.machine.object.Section stackMapSection = objectFile.getSection(".llvm_stackmaps");
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

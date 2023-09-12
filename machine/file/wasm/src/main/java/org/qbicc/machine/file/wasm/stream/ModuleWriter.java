package org.qbicc.machine.file.wasm.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Data;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.Wasm;
import org.qbicc.machine.file.wasm.Mutability;
import org.qbicc.machine.file.wasm.ValType;

/**
 *
 */
public final class ModuleWriter extends ModuleVisitor<IOException> {
    private final WasmOutputStream os;

    ModuleWriter(WasmOutputStream os) throws IOException {
        this.os = os;
        os.rawInt(0x6D_73_61_00); // magic
        os.rawInt(1); // version
    }

    public static ModuleWriter forStream(OutputStream os) throws IOException {
        Assert.checkNotNullParam("os", os);
        return new ModuleWriter(new WasmOutputStream(os));
    }

    public static ModuleWriter forFile(Path path) throws IOException {
        Assert.checkNotNullParam("path", path);
        return new ModuleWriter(new WasmOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)));
    }

    @Override
    public TypeSectionVisitor<IOException> visitTypeSection() {
        return new TypeSectionVisitor<IOException>() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            private final WasmOutputStream wos = new WasmOutputStream(baos);
            private int count = 0;

            @Override
            public void visitType(FuncType functionType) throws IOException {
                Assert.checkNotNullParam("functionType", functionType);
                count ++;
                wos.rawByte(0x60);
                wos.u32(functionType.parameterTypes().size());
                for (ValType wasmType : functionType.parameterTypes()) {
                    wos.type(wasmType);
                }
                wos.u32(functionType.resultTypes().size());
                for (ValType wasmType : functionType.resultTypes()) {
                    wos.type(wasmType);
                }
            }

            @Override
            public void visitEnd() throws IOException {
                wos.close();
                endSection(count, Wasm.SECTION_TYPE, baos);
            }
        };
    }

    @Override
    public ImportSectionVisitor<IOException> visitImportSection() {
        return new ImportSectionVisitor<IOException>() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            private final WasmOutputStream wos = new WasmOutputStream(baos);
            private int count = 0;

            @Override
            public void visitFunctionImport(String moduleName, String name, int typeIdx) throws IOException {
                Assert.checkNotNullParam("moduleName", moduleName);
                Assert.checkNotNullParam("name", name);
                Assert.checkMinimumParameter("typeIdx", 0, typeIdx);
                count ++;
                wos.utf8(moduleName);
                wos.utf8(name);
                wos.rawByte(0x00);
                wos.u32(typeIdx);
            }

            @Override
            public void visitTableImport(String moduleName, String name, RefType type, int min) throws IOException {
                Assert.checkNotNullParam("moduleName", moduleName);
                Assert.checkNotNullParam("name", name);
                Assert.checkNotNullParam("type", type);
                Assert.checkMinimumParameter("min", 0, min);
                count ++;
                wos.utf8(moduleName);
                wos.utf8(name);
                wos.rawByte(0x01);
                wos.type(type);
                wos.rawByte(0x00);
                wos.u32(min);
            }

            @Override
            public void visitTableImport(String moduleName, String name, RefType type, int min, int max) throws IOException {
                Assert.checkNotNullParam("moduleName", moduleName);
                Assert.checkNotNullParam("name", name);
                Assert.checkNotNullParam("type", type);
                Assert.checkMinimumParameter("min", 0, min);
                Assert.checkMinimumParameter("max", min, max);
                count ++;
                wos.utf8(moduleName);
                wos.utf8(name);
                wos.rawByte(0x01);
                wos.type(type);
                wos.rawByte(0x01);
                wos.u32(min);
                wos.u32(max);
            }

            @Override
            public void visitMemoryImport(String moduleName, String name, int min) throws IOException {
                Assert.checkNotNullParam("moduleName", moduleName);
                Assert.checkNotNullParam("name", name);
                Assert.checkMinimumParameter("min", 0, min);
                count ++;
                wos.utf8(moduleName);
                wos.utf8(name);
                wos.rawByte(0x02);
                wos.rawByte(0x00);
                wos.u32(min);
            }

            @Override
            public void visitMemoryImport(String moduleName, String name, int min, int max) throws IOException {
                Assert.checkNotNullParam("moduleName", moduleName);
                Assert.checkNotNullParam("name", name);
                Assert.checkMinimumParameter("min", 0, min);
                Assert.checkMinimumParameter("max", min, max);
                count ++;
                wos.utf8(moduleName);
                wos.utf8(name);
                wos.rawByte(0x02);
                wos.rawByte(0x01);
                wos.u32(min);
                wos.u32(max);
            }

            @Override
            public void visitGlobalImport(String moduleName, String name, ValType type, Mutability mut) throws IOException {
                Assert.checkNotNullParam("moduleName", moduleName);
                Assert.checkNotNullParam("name", name);
                Assert.checkNotNullParam("type", type);
                Assert.checkNotNullParam("mut", mut);
                count ++;
                wos.utf8(moduleName);
                wos.utf8(name);
                wos.rawByte(0x03);
                wos.type(type);
                wos.mut(mut);
            }

            @Override
            public void visitEnd() throws IOException {
                wos.close();
                endSection(count, Wasm.SECTION_IMPORT, baos);
            }
        };
    }

    @Override
    public FunctionSectionVisitor<IOException> visitFunctionSection() throws IOException {
        return new FunctionSectionVisitor<IOException>() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            private final WasmOutputStream wos = new WasmOutputStream(baos);
            private int count = 0;

            @Override
            public void visitFunction(int typeIdx) throws IOException {
                count ++;
                wos.u32(typeIdx);
            }

            @Override
            public void visitEnd() throws IOException {
                wos.close();
                endSection(count, Wasm.SECTION_FUNCTION, baos);
            }
        };
    }

    @Override
    public TableSectionVisitor<IOException> visitTableSection() throws IOException {
        return new TableSectionVisitor<IOException>() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            private final WasmOutputStream wos = new WasmOutputStream(baos);
            private int count = 0;

            @Override
            public void visitTable(RefType type, int min) throws IOException {
                Assert.checkNotNullParam("type", type);
                count ++;
                wos.type(type);
                wos.rawByte(0x00);
                wos.u32(min);
            }

            @Override
            public void visitTable(RefType type, int min, int max) throws IOException {
                Assert.checkNotNullParam("type", type);
                Assert.checkMinimumParameterUnsigned("max", min, max);
                count ++;
                wos.type(type);
                wos.rawByte(0x01);
                wos.u32(min);
                wos.u32(max);
            }

            @Override
            public void visitEnd() throws IOException {
                wos.close();
                endSection(count, Wasm.SECTION_TABLE, baos);
            }
        };
    }

    @Override
    public MemorySectionVisitor<IOException> visitMemorySection() throws IOException {
        return new MemorySectionVisitor<IOException>() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            private final WasmOutputStream wos = new WasmOutputStream(baos);
            private int count = 0;

            @Override
            public void visitMemory(int min) throws IOException {
                Assert.checkMinimumParameter("min", 0, min);
                count ++;
                wos.rawByte(0x00);
                wos.u32(min);
            }

            @Override
            public void visitMemory(int min, int max) throws IOException {
                Assert.checkMinimumParameter("min", 0, min);
                Assert.checkMinimumParameter("max", min, max);
                count ++;
                wos.rawByte(0x01);
                wos.u32(min);
                wos.u32(max);
            }

            @Override
            public void visitEnd() throws IOException {
                wos.close();
                endSection(count, Wasm.SECTION_MEMORY, baos);
            }
        };
    }

    @Override
    public GlobalSectionVisitor<IOException> visitGlobalSection() throws IOException {
        return new GlobalSectionVisitor<IOException>() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            private final WasmOutputStream wos = new WasmOutputStream(baos);
            private int count = 0;

            @Override
            public InsnSeqVisitor<IOException> visitGlobal(ValType type, Mutability mut) throws IOException {
                Assert.checkNotNullParam("type", type);
                Assert.checkNotNullParam("mut", mut);
                count ++;
                wos.type(type);
                wos.mut(mut);
                return new WriteExprVisitor(wos);
            }

            @Override
            public void visitEnd() throws IOException {
                wos.close();
                endSection(count, Wasm.SECTION_GLOBAL, baos);
            }
        };
    }

    @Override
    public ExportSectionVisitor<IOException> visitExportSection() throws IOException {
        return new ExportSectionVisitor<IOException>() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            private final WasmOutputStream wos = new WasmOutputStream(baos);
            private int count = 0;

            @Override
            public void visitFunctionExport(String name, int funcIdx) throws IOException {
                Assert.checkNotNullParam("name", name);
                Assert.checkMinimumParameter("funcIdx", 0, funcIdx);
                count ++;
                wos.utf8(name);
                wos.rawByte(0x00);
                wos.u32(funcIdx);
            }

            @Override
            public void visitTableExport(String name, int tableIdx) throws IOException {
                Assert.checkNotNullParam("name", name);
                Assert.checkMinimumParameter("tableIdx", 0, tableIdx);
                count ++;
                wos.utf8(name);
                wos.rawByte(0x01);
                wos.u32(tableIdx);
                super.visitTableExport(name, tableIdx);
            }

            @Override
            public void visitMemoryExport(String name, int memIdx) throws IOException {
                Assert.checkNotNullParam("name", name);
                Assert.checkMinimumParameter("memIdx", 0, memIdx);
                count ++;
                wos.utf8(name);
                wos.rawByte(0x02);
                wos.u32(memIdx);
            }

            @Override
            public void visitGlobalExport(String name, int globalIdx) throws IOException {
                Assert.checkNotNullParam("name", name);
                Assert.checkMinimumParameter("globalIdx", 0, globalIdx);
                count ++;
                wos.utf8(name);
                wos.rawByte(0x03);
                wos.u32(globalIdx);
            }

            @Override
            public void visitEnd() throws IOException {
                wos.close();
                endSection(count, Wasm.SECTION_EXPORT, baos);
            }
        };
    }

    @Override
    public StartSectionVisitor<IOException> visitStartSection() throws IOException {
        return new StartSectionVisitor<IOException>() {
            int index;
            boolean found;

            @Override
            public void visitStartFunction(int index) throws IOException {
                if (found) {
                    throw new IOException("Start function written multiple times");
                }
                this.index = index;
                found = true;
            }

            @Override
            public void visitEnd() throws IOException {
                if (found) {
                    os.rawByte(Wasm.SECTION_START);
                    os.u32(Wasm.uleb128_size(index));
                    os.u32(index);
                }
            }
        };
    }

    @Override
    public ElementSectionVisitor<IOException> visitElementSection() throws IOException {
        return new ElementSectionVisitor<IOException>() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            private final WasmOutputStream wos = new WasmOutputStream(baos);
            private int count = 0;

            @Override
            public ElementVisitor<IOException> visitPassiveElement() throws IOException {
                return new ElementVisitor<IOException>() {
                    private RefType type = RefType.funcref;
                    private final ByteArrayOutputStream ibaos = new ByteArrayOutputStream(64);
                    private final WasmOutputStream iwos = new WasmOutputStream(ibaos);
                    int initCnt;
                    int[] initVec;
                    boolean initInProgress;

                    @Override
                    public void visitType(RefType refType) throws IOException {
                        checkInit();
                        this.type = refType;
                    }

                    @Override
                    public void visitInit(int... funcIdx) throws IOException {
                        checkInit();
                        initVec = funcIdx;
                    }

                    @Override
                    public ElementInitVisitor<IOException> visitInit() throws IOException {
                        return new ElementInitVisitor<>() {
                            @Override
                            public InsnSeqVisitor<IOException> visitInit() {
                                checkInit();
                                initInProgress = true;
                                initCnt ++;
                                return new WriteExprVisitor(iwos) {
                                    @Override
                                    public void visitEnd() throws IOException {
                                        initInProgress = false;
                                        super.visitEnd();
                                    }
                                };
                            }
                        };
                    }

                    private void checkInit() {
                        if (initInProgress) {
                            throw new IllegalStateException("Previous init not completed");
                        }
                    }

                    @Override
                    public void visitEnd() throws IOException {
                        checkInit();
                        count ++;
                        if (initVec != null) {
                            wos.rawByte(0b001);
                            wos.rawByte(0x00);
                            wos.u32(initVec.length);
                            for (int init : initVec) {
                                wos.u32(init);
                            }
                        } else {
                            iwos.close();
                            wos.rawByte(0b101);
                            wos.type(type);
                            wos.u32(initCnt);
                            wos.rawBytes(ibaos.toByteArray());
                        }
                    }
                };
            }

            @Override
            public ElementVisitor<IOException> visitDeclarativeElement() throws IOException {
                return new ElementVisitor<IOException>() {
                    private RefType type = RefType.funcref;
                    private final ByteArrayOutputStream ibaos = new ByteArrayOutputStream(64);
                    private final WasmOutputStream iwos = new WasmOutputStream(ibaos);
                    int initCnt;
                    int[] initVec;
                    boolean initInProgress;

                    @Override
                    public void visitType(RefType refType) throws IOException {
                        this.type = refType;
                    }

                    @Override
                    public void visitInit(int... funcIdx) throws IOException {
                        initVec = funcIdx;
                    }

                    @Override
                    public ElementInitVisitor<IOException> visitInit() throws IOException {
                        checkInit();
                        return new ElementInitVisitor<>() {
                            @Override
                            public InsnSeqVisitor<IOException> visitInit() {
                                checkInit();
                                initInProgress = true;
                                initCnt ++;
                                return new WriteExprVisitor(iwos) {
                                    @Override
                                    public void visitEnd() throws IOException {
                                        initInProgress = false;
                                        super.visitEnd();
                                    }
                                };
                            }
                        };
                    }

                    private void checkInit() {
                        if (initInProgress) {
                            throw new IllegalStateException("Previous init not completed");
                        }
                    }

                    @Override
                    public void visitEnd() throws IOException {
                        count ++;
                        if (initVec != null) {
                            wos.rawByte(0b011);
                            wos.rawByte(0x00);
                            wos.u32(initVec.length);
                            for (int init : initVec) {
                                wos.u32(init);
                            }
                        } else {
                            iwos.close();
                            wos.rawByte(0b111);
                            wos.type(type);
                            wos.u32(initCnt);
                            wos.rawBytes(ibaos.toByteArray());
                        }
                    }
                };
            }

            @Override
            public ActiveElementVisitor<IOException> visitActiveElement() throws IOException {
                return new ActiveElementVisitor<>() {
                    private RefType type = RefType.funcref;
                    private final ByteArrayOutputStream obaos = new ByteArrayOutputStream(64);
                    private final WasmOutputStream owos = new WasmOutputStream(obaos);
                    private final ByteArrayOutputStream ibaos = new ByteArrayOutputStream(64);
                    private final WasmOutputStream iwos = new WasmOutputStream(ibaos);
                    int tableIdx;
                    int initCnt;
                    int[] initVec;
                    boolean initInProgress;

                    @Override
                    public void visitTableIndex(int index) throws IOException {
                        tableIdx = index;
                    }

                    @Override
                    public InsnSeqVisitor<IOException> visitOffset() throws IOException {
                        return new WriteExprVisitor(owos);
                    }

                    @Override
                    public void visitType(RefType refType) throws IOException {
                        checkInit();
                        this.type = refType;
                    }

                    @Override
                    public void visitInit(int... funcIdx) throws IOException {
                        checkInit();
                        initVec = funcIdx;
                    }

                    @Override
                    public ElementInitVisitor<IOException> visitInit() throws IOException {
                        checkInit();
                        return new ElementInitVisitor<>() {
                            @Override
                            public InsnSeqVisitor<IOException> visitInit() {
                                checkInit();
                                initInProgress = true;
                                initCnt++;
                                return new WriteExprVisitor(iwos) {
                                    @Override
                                    public void visitEnd() throws IOException {
                                        initInProgress = false;
                                        super.visitEnd();
                                    }
                                };
                            }
                        };
                    }

                    private void checkInit() {
                        if (initInProgress) {
                            throw new IllegalStateException("Previous init not completed");
                        }
                    }

                    @Override
                    public void visitEnd() throws IOException {
                        checkInit();
                        owos.close();
                        count ++;
                        if (initVec != null) {
                            if (tableIdx != 0 || type != RefType.funcref) {
                                wos.rawByte(0b010);
                                wos.u32(tableIdx);
                                wos.rawBytes(obaos.toByteArray());
                                wos.type(type);
                            } else {
                                wos.rawByte(0b000);
                                wos.rawBytes(obaos.toByteArray());
                            }
                            wos.u32(initVec.length);
                            for (int init : initVec) {
                                wos.u32(init);
                            }
                        } else {
                            iwos.close();
                            if (tableIdx != 0 || type != RefType.funcref) {
                                wos.rawByte(0b110);
                                wos.u32(tableIdx);
                                wos.rawBytes(obaos.toByteArray());
                                wos.type(type);
                            } else {
                                wos.rawByte(0b100);
                                wos.rawBytes(obaos.toByteArray());
                            }
                            wos.u32(initCnt);
                            wos.rawBytes(ibaos.toByteArray());
                        }
                    }
                };
            }

            @Override
            public void visitEnd() throws IOException {
                wos.close();
                endSection(count, Wasm.SECTION_ELEMENT, baos);
            }
        };
    }

    @Override
    public CodeSectionVisitor<IOException> visitCodeSection() throws IOException {
        return new CodeSectionVisitor<IOException>() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            private final WasmOutputStream wos = new WasmOutputStream(baos);
            private int count = 0;

            @Override
            public CodeVisitor<IOException> visitCode() throws IOException {
                return new CodeVisitor<>() {
                    private final ByteArrayOutputStream ibaos = new ByteArrayOutputStream(256);
                    private final WasmOutputStream iwos = new WasmOutputStream(ibaos);
                    private int lc = 0;

                    @Override
                    public void visitLocal(int count, ValType type) throws IOException {
                        Assert.checkNotNullParam("type", type);
                        iwos.u32(count);
                        iwos.type(type);
                        lc ++;
                    }

                    @Override
                    public InsnSeqVisitor<IOException> visitBody() throws IOException {
                        return new WriteExprVisitor(iwos);
                    }

                    @Override
                    public void visitEnd() throws IOException {
                        iwos.close();
                        byte[] bytes = ibaos.toByteArray();
                        wos.u32(bytes.length + Wasm.uleb128_size(lc));
                        // vec(locals) size
                        wos.u32(lc);
                        // locals followed by expr
                        wos.rawBytes(bytes);
                        count ++;
                    }
                };
            }

            @Override
            public void visitEnd() throws IOException {
                wos.close();
                endSection(count, Wasm.SECTION_CODE, baos);
            }
        };
    }

    @Override
    public DataSectionVisitor<IOException> visitDataSection() throws IOException {
        return new DataSectionVisitor<>() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            private final WasmOutputStream wos = new WasmOutputStream(baos);
            private int count = 0;

            @Override
            public DataVisitor<IOException> visitPassiveSegment() throws IOException {
                wos.u32(0x01);
                return new DataVisitor<>() {
                    @Override
                    public void visitData(Data data) throws IOException {
                        wos.byteVec(data.asBytes());
                    }

                    @Override
                    public void visitEnd() throws IOException {
                        count++;
                    }
                };
            }

            @Override
            public ActiveDataVisitor<IOException> visitActiveSegment(int memIdx) throws IOException {
                if (memIdx == 0) {
                    wos.u32(0);
                } else {
                    wos.u32(2);
                    wos.u32(memIdx);
                }
                return new ActiveDataVisitor<>() {
                    @Override
                    public InsnSeqVisitor<IOException> visitOffset() throws IOException {
                        return new WriteExprVisitor(wos);
                    }

                    @Override
                    public void visitData(Data data) throws IOException {
                        wos.byteVec(data.asBytes());
                    }

                    @Override
                    public void visitEnd() throws IOException {
                        count++;
                    }
                };
            }

            @Override
            public void visitEnd() throws IOException {
                wos.close();
                endSection(count, Wasm.SECTION_DATA, baos);
            }
        };
    }

    @Override
    public DataCountSectionVisitor<IOException> visitDataCountSection() throws IOException {
        return new DataCountSectionVisitor<>() {
            int dataCount = -1;

            @Override
            public void visitDataCount(int dataCount) throws IOException {
                this.dataCount = dataCount;
            }

            @Override
            public void visitEnd() throws IOException {
                os.rawByte(Wasm.SECTION_DATA_COUNT);
                os.u32(Wasm.uleb128_size(dataCount));
                os.u32(dataCount);
            }
        };
    }

    private static class WriteExprVisitor extends InsnSeqVisitor<IOException> {

        private final WasmOutputStream wos;

        WriteExprVisitor(final WasmOutputStream wos) {
            this.wos = wos;
        }

        private void writeOpcode(Op insn) throws IOException {
            int prefix = insn.prefix();
            if (prefix != -1) {
                wos.rawByte(insn.prefix());
                wos.u32(insn.opcode());
            } else {
                wos.rawByte(insn.opcode());
            }
        }

        @Override
        public void visit(Op.Simple insn) throws IOException {
            writeOpcode(insn);
        }

        @Override
        public void visit(Op.Data insn, int dataIdx) throws IOException {
            writeOpcode(insn);
            wos.u32(dataIdx);
        }

        @Override
        public void visit(Op.Memory insn, int memory) throws IOException {
            writeOpcode(insn);
            wos.u32(memory);
        }

        @Override
        public void visit(Op.MemoryAccess insn, int memory, int align, int offset) throws IOException {
            writeOpcode(insn);
            if (memory != 0) {
                // xxx
            }
            if (Integer.bitCount(align) != 1) {
                throw new IllegalArgumentException("Invalid alignment");
            }
            wos.u32(Integer.numberOfTrailingZeros(align));
            wos.u32(offset);
        }

        @Override
        public void visit(Op.MemoryAndData insn, int dataIdx, int memIdx) throws IOException {
            writeOpcode(insn);
            wos.u32(dataIdx);
            wos.u32(memIdx);
        }

        @Override
        public void visit(Op.MemoryAndMemory insn, int memIdx1, int memIdx2) throws IOException {
            writeOpcode(insn);
            wos.u32(memIdx1);
            wos.u32(memIdx2);
        }

        @Override
        public void visit(Op.Block insn, int typeIdx) throws IOException {
            writeOpcode(insn);
            wos.sleb128(typeIdx & 0xFFFF_FFFFL);
        }

        @Override
        public void visit(Op.Block insn, ValType valType) throws IOException {
            writeOpcode(insn);
            wos.type(valType);
        }

        @Override
        public void visit(Op.Block insn) throws IOException {
            writeOpcode(insn);
            wos.rawByte(0x40);
        }

        @Override
        public void visit(Op.Element insn, int elemIdx) throws IOException {
            writeOpcode(insn);
            wos.u32(elemIdx);
        }

        @Override
        public void visit(Op.ElementAndTable insn, int elemIdx, int tableIdx) throws IOException {
            writeOpcode(insn);
            wos.u32(elemIdx);
            wos.u32(tableIdx);
        }

        @Override
        public void visit(Op.Func insn, int funcIdx) throws IOException {
            writeOpcode(insn);
            wos.u32(funcIdx);
        }

        @Override
        public void visit(Op.Global insn, int globalIdx) throws IOException {
            writeOpcode(insn);
            wos.u32(globalIdx);
        }

        @Override
        public void visit(Op.Local insn, int index) throws IOException {
            writeOpcode(insn);
            wos.u32(index);
        }

        @Override
        public void visit(Op.Branch insn, int index) throws IOException {
            writeOpcode(insn);
            wos.u32(index);
        }

        @Override
        public void visit(Op.Lane insn, int laneIdx) throws IOException {
            writeOpcode(insn);
            wos.u32(laneIdx);
        }

        @Override
        public void visit(Op.MultiBranch insn, int defIndex, int... targetIndexes) throws IOException {
            writeOpcode(insn);
            wos.u32(targetIndexes.length);
            for (int targetIndex : targetIndexes) {
                wos.u32(targetIndex);
            }
            wos.u32(defIndex);
        }

        @Override
        public void visit(Op.Table insn, int index) throws IOException {
            writeOpcode(insn);
            wos.u32(index);
        }

        @Override
        public void visit(Op.Types insn, ValType... types) throws IOException {
            if (types.length == 0) {
                visit(insn.asSimple());
            } else {
                writeOpcode(insn);
                wos.u32(types.length);
                for (ValType type : types) {
                    wos.type(type);
                }
            }
        }

        @Override
        public void visit(Op.TableAndTable insn, int index1, int index2) throws IOException {
            writeOpcode(insn);
            wos.u32(index1);
            wos.u32(index2);
        }

        @Override
        public void visit(Op.TableAndFuncType insn, int tableIdx, int typeIdx) throws IOException {
            writeOpcode(insn);
            wos.u32(typeIdx);
            wos.u32(tableIdx);
        }

        @Override
        public void visit(Op.RefTyped insn, RefType type) throws IOException {
            writeOpcode(insn);
            wos.type(type);
        }

        @Override
        public void visit(Op.ConstI32 insn, int val) throws IOException {
            writeOpcode(insn);
            wos.u32(val);
        }

        @Override
        public void visit(Op.ConstF32 insn, float val) throws IOException {
            writeOpcode(insn);
            wos.f32(val);
        }

        @Override
        public void visit(Op.ConstI64 insn, long val) throws IOException {
            writeOpcode(insn);
            wos.u64(val);
        }

        @Override
        public void visit(Op.ConstF64 insn, double val) throws IOException {
            writeOpcode(insn);
            wos.f64(val);
        }

        @Override
        public void visit(Op.ConstI128 insn, long lowVal, long highVal) throws IOException {
            writeOpcode(insn);
            wos.u128(lowVal, highVal);
        }

        @Override
        public void visit(Op.MemoryAccessLane insn, int memory, int align, int offset, int laneIdx) throws IOException {
            writeOpcode(insn);
            if (memory != 0) {
                // xxx
            }
            if (Integer.bitCount(align) != 1) {
                throw new IllegalArgumentException("Invalid alignment");
            }
            wos.u32(Integer.numberOfTrailingZeros(align));
            wos.u32(offset);
            wos.u32(laneIdx);
        }

        @Override
        public void visitEnd() throws IOException {
            // no operation needed (must end with an end instruction to be valid)
        }
    }

    private void endSection(int count, int sectionId, ByteArrayOutputStream byteData) throws IOException {
        if (count == 0) {
            // do not write the section
            return;
        }
        os.rawByte(sectionId);
        byte[] data = byteData.toByteArray();
        os.u32(data.length + Wasm.uleb128_size(count));
        os.u32(count);
        os.rawBytes(data);
    }

    @Override
    public void visitEnd() throws IOException {
        os.close();
    }

}

package org.qbicc.machine.file.wasm.stream;

import static org.qbicc.machine.file.wasm.Opcodes.*;
import static org.qbicc.machine.file.wasm.Wasm.*;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Data;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.Mutability;
import org.qbicc.machine.file.wasm.Ops;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.TagAttribute;
import org.qbicc.machine.file.wasm.ValType;

/**
 * A reader for WASM binary files.
 */
public final class ModuleReader implements Closeable {
    private final WasmInputStream is;

    ModuleReader(WasmInputStream is) {
        this.is = is;
    }

    public static ModuleReader forStream(InputStream inputStream) {
        Assert.checkNotNullParam("inputStream", inputStream);
        return new ModuleReader(new WasmInputStream(inputStream));
    }

    public static ModuleReader forFile(Path path) throws IOException {
        Assert.checkNotNullParam("path", path);
        return new ModuleReader(new WasmInputStream(Files.newInputStream(path)));
    }

    public static ModuleReader forBytes(byte[] data) {
        Assert.checkNotNullParam("data", data);
        return new ModuleReader(new WasmInputStream(new ByteArrayInputStream(data)));
    }

    public <E extends Exception> void accept(ModuleVisitor<E> visitor) throws IOException, E {
        // read header
        int magic = is.rawInt();
        if (magic != 0x6D_73_61_00) { // "\0asm", little-endian
            throw new IOException("Invalid magic number");
        }
        int version = is.rawInt();
        if (version != 1) {
            throw new IOException("Unsupported version " + version);
        }
        // read each section
        int section, lastSection = -1;
        while ((section = is.optByte()) != -1) {
            int size = is.u32();
            if (section != 0) {
                if (lastSection != -1) {
                    if (Section.forId(section).precedes(Section.forId(lastSection))) {
                        throw new IOException("Wrong section order: " + Integer.toHexString(section) + " came after " + Integer.toHexString(lastSection));
                    }
                }
                lastSection = section;
            }
            Visitor<E> sv = switch (section) {
                case SECTION_TYPE -> parse(visitor.visitTypeSection(), size);
                case SECTION_IMPORT -> parse(visitor.visitImportSection(), size);
                case SECTION_FUNCTION -> parse(visitor.visitFunctionSection(), size);
                case SECTION_TABLE -> parse(visitor.visitTableSection(), size);
                case SECTION_MEMORY -> parse(visitor.visitMemorySection(), size);
                case SECTION_GLOBAL -> parse(visitor.visitGlobalSection(), size);
                case SECTION_EXPORT -> parse(visitor.visitExportSection(), size);
                case SECTION_START -> parse(visitor.visitStartSection(), size);
                case SECTION_ELEMENT -> parse(visitor.visitElementSection(), size);
                case SECTION_CODE -> parse(visitor.visitCodeSection(), size);
                case SECTION_DATA -> parse(visitor.visitDataSection(), size);
                case SECTION_DATA_COUNT -> parse(visitor.visitDataCountSection(), size);
                case SECTION_TAG -> parse(visitor.visitTagSection(), size);
                case SECTION_CUSTOM -> skipSection(size);
                default -> throw new IOException("Unknown section ID 0x" + Integer.toHexString(section));
            };
            if (sv != null) {
                sv.visitEnd();
            }
        }
        visitor.visitEnd();
    }

    private <E extends Exception> Visitor<E> skipSection(final int size) throws IOException {
        is.skip(size);
        return null;
    }

    private <E extends Exception> Visitor<E> parse(final TypeSectionVisitor<E> sv, int size) throws IOException, E {
        if (sv == null) {
            is.skip(size);
            return null;
        }
        int cnt = is.u32();
        for (int i = 0; i < cnt; i++) {
            sv.visitType(is.funcType());
        }
        return sv;
    }

    private <E extends Exception> Visitor<E> parse(final ImportSectionVisitor<E> sv, final int size) throws IOException, E {
        if (sv == null) {
            is.skip(size);
            return null;
        }
        int cnt = is.u32();
        for (int i = 0; i < cnt; i++) {
            String moduleName = is.utf8();
            String name = is.utf8();
            int kind = is.rawByte();
            switch (kind) {
                case 0x00 -> sv.visitFunctionImport(moduleName, name, is.u32());
                case 0x01 -> {
                    RefType rt = is.refType();
                    switch (is.rawByte()) {
                        case 0x00 -> sv.visitTableImport(moduleName, name, rt, is.u32());
                        case 0x01 -> sv.visitTableImport(moduleName, name, rt, is.u32(), is.u32(), false);
                        case 0x03 -> sv.visitTableImport(moduleName, name, rt, is.u32(), is.u32(), true);
                        default -> throw new IOException("Expected valid limits type for table import");
                    }
                }
                case 0x02 -> {
                    switch (is.rawByte()) {
                        case 0x00 -> sv.visitMemoryImport(moduleName, name, is.u32());
                        case 0x01 -> sv.visitMemoryImport(moduleName, name, is.u32(), is.u32(), false);
                        case 0x03 -> sv.visitMemoryImport(moduleName, name, is.u32(), is.u32(), true);
                        default -> throw new IOException("Expected valid limits type for memory import");
                    }
                }
                case 0x03 -> sv.visitGlobalImport(moduleName, name, is.type(), switch (is.rawByte()) {
                    case 0x00 -> Mutability.const_;
                    case 0x01 -> Mutability.var_;
                    default -> throw new IOException("Expected valid mutability type for global import");
                });
                case 0x04 -> sv.visitTagImport(moduleName, name, TagAttribute.values()[is.u32()], is.u32());
                default -> throw new IOException("Expected valid type for import");
            }
        }
        return sv;
    }

    private <E extends Exception> Visitor<E> parse(final FunctionSectionVisitor<E> sv, final int size) throws IOException, E {
        if (sv == null) {
            is.skip(size);
            return null;
        }
        int cnt = is.u32();
        for (int i = 0; i < cnt; i++) {
            sv.visitFunction(is.u32());
        }
        return sv;
    }

    private <E extends Exception> Visitor<E> parse(final TableSectionVisitor<E> sv, final int size) throws IOException, E {
        if (sv == null) {
            is.skip(size);
            return null;
        }
        int cnt = is.u32();
        for (int i = 0; i < cnt; i++) {
            RefType type = is.refType();
            switch (is.rawByte()) {
                case 0x00 -> sv.visitTable(type, is.u32());
                case 0x01 -> sv.visitTable(type, is.u32(), is.u32());
                default -> throw new IOException("Expected valid limits type for table");
            }
        }
        return sv;
    }

    private <E extends Exception> Visitor<E> parse(final MemorySectionVisitor<E> sv, final int size) throws IOException, E {
        if (sv == null) {
            is.skip(size);
            return null;
        }
        int cnt = is.u32();
        for (int i = 0; i < cnt; i++) {
            switch (is.rawByte()) {
                case 0x00 -> sv.visitMemory(is.u32());
                case 0x01 -> sv.visitMemory(is.u32(), is.u32());
                default -> throw new IOException("Expected valid limits type for memory");
            }
        }
        return sv;
    }

    private <E extends Exception> Visitor<E> parse(final GlobalSectionVisitor<E> sv, final int size) throws IOException, E {
        if (sv == null) {
            is.skip(size);
            return null;
        }
        int cnt = is.u32();
        for (int i = 0; i < cnt; i++) {
            ValType type = is.type();
            Mutability mut = is.mut();
            InsnSeqVisitor<E> ev = sv.visitGlobal(type, mut);
            parse(ev);
        }
        return sv;
    }

    private <E extends Exception> Visitor<E> parse(final ExportSectionVisitor<E> sv, final int size) throws IOException, E {
        if (sv == null) {
            is.skip(size);
            return null;
        }
        int cnt = is.u32();
        for (int i = 0; i < cnt; i++) {
            String name = is.utf8();
            int type = is.rawByte();
            int idx = is.u32();
            switch (type) {
                case 0x00 -> sv.visitFunctionExport(name, idx);
                case 0x01 -> sv.visitTableExport(name, idx);
                case 0x02 -> sv.visitMemoryExport(name, idx);
                case 0x03 -> sv.visitGlobalExport(name, idx);
                default -> throw new IOException("Expected valid export descriptor type for export");
            }
        }
        return sv;
    }

    private <E extends Exception> Visitor<E> parse(final StartSectionVisitor<E> sv, final int size) throws IOException, E {
        if (sv == null) {
            is.skip(size);
            return null;
        }
        sv.visitStartFunction(is.u32());
        return sv;
    }

    private <E extends Exception> Visitor<E> parse(final ElementSectionVisitor<E> sv, final int size) throws IOException, E {
        if (sv == null) {
            is.skip(size);
            return null;
        }
        int cnt = is.u32();
        for (int i = 0; i < cnt; i ++) {
            int entryType = is.u32();
            boolean active = (entryType & 0b001) == 0;
            boolean declarative = ! active && (entryType & 0b010) != 0;
            boolean hasTableIdx = (entryType & 0b010) != 0;
            boolean typeAndExpr = (entryType & 0b100) != 0;
            ElementVisitor<E> ev;
            if (active) {
                // table idx
                ActiveElementVisitor<E> aev = Objects.requireNonNullElseGet(sv.visitActiveElement(), ActiveElementVisitor::new);
                if (hasTableIdx) {
                    int ti = is.u32();
                    aev.visitTableIndex(ti);
                } else {
                    aev.visitTableIndex(0);
                }
                // offset
                InsnSeqVisitor<E> isv = Objects.requireNonNullElseGet(aev.visitOffset(), InsnSeqVisitor::new);
                parse(isv);
                isv.visitEnd();
                ev = aev;
            } else if (declarative) {
                ev = Objects.requireNonNullElseGet(sv.visitDeclarativeElement(), ElementVisitor::new);
            } else {
                ev = Objects.requireNonNullElseGet(sv.visitPassiveElement(), ElementVisitor::new);
            }
            // type
            if (active && ! hasTableIdx) {
                // always funcref
                ev.visitType(RefType.funcref);
            } else if (typeAndExpr) {
                RefType refType = is.refType();
                ev.visitType(refType);
            } else {
                if (is.rawByte() != 0x00) {
                    throw new IOException("Invalid element kind");
                }
                // still always funcref
                ev.visitType(RefType.funcref);
            }
            // init
            if (typeAndExpr) {
                int initCnt = is.u32();
                ElementInitVisitor<E> iv = Objects.requireNonNullElseGet(ev.visitInit(), ElementInitVisitor::new);
                for (int j = 0; j < initCnt; j ++) {
                    InsnSeqVisitor<E> isv = Objects.requireNonNullElseGet(iv.visitInit(), InsnSeqVisitor::new);
                    parse(isv);
                    isv.visitEnd();
                }
                iv.visitEnd();
            } else {
                ev.visitInit(is.u32Vec());
            }
            ev.visitEnd();
        }
        return sv;
    }

    private <E extends Exception> Visitor<E> parse(final CodeSectionVisitor<E> sv, final int size) throws IOException, E {
        if (sv == null) {
            is.skip(size);
            return null;
        }
        int cnt = is.u32();
        for (int i = 0; i < cnt; i ++) {
            int codeSize = is.u32();
            CodeVisitor<E> cv = sv.visitCode();
            if (cv == null) {
                is.skip(codeSize);
            } else {
                int lc = is.u32();
                for (int j = 0; j < lc; j ++) {
                    cv.visitLocal(is.u32(), is.type());
                }
                parse(cv.visitBody());
                cv.visitEnd();
            }
        }
        return sv;
    }

    private <E extends Exception> Visitor<E> parse(final DataSectionVisitor<E> sv, final int size) throws IOException, E {
        if (sv == null) {
            is.skip(size);
            return null;
        }
        int cnt = is.u32();
        for (int i = 0; i < cnt; i ++) {
            int kind = is.u32();
            int memIdx = kind == 0x02 ? is.u32() : 0;
            DataVisitor<E> dv;
            if (kind == 0x01) {
                dv = sv.visitPassiveSegment();
            } else if (kind == 0x00 || kind == 0x02) {
                ActiveDataVisitor<E> adv = sv.visitActiveSegment(memIdx);
                parse(adv == null ? null : adv.visitOffset());
                dv = adv;
            } else {
                throw new IOException("Invalid data section entry kind");
            }
            if (dv == null) {
                is.skip(is.u32());
            } else {
                dv.visitData(Data.of(is.byteVec()));
                dv.visitEnd();
            }
        }
        return sv;
    }

    private <E extends Exception> Visitor<E> parse(final DataCountSectionVisitor<E> sv, final int size) throws IOException, E {
        if (sv == null) {
            is.skip(size);
            return null;
        }
        sv.visitDataCount(is.u32());
        return sv;
    }

    private <E extends Exception> Visitor<E> parse(final TagSectionVisitor<E> sv, final int size) throws IOException, E {
        if (sv == null) {
            is.skip(size);
            return null;
        }
        int cnt = is.u32();
        for (int i = 0; i < cnt; i ++) {
            sv.visitTag(TagAttribute.values()[is.u32()], is.u32());
        }
        return sv;
    }

    private <E extends Exception> void parse(InsnSeqVisitor<E> ev) throws IOException, E {
        if (ev == null) {
            // just consume it
            ev = new InsnSeqVisitor<>();
        }
        int opcode;
        int prefix;
        int nesting = 0;
        for (;;) {
            prefix = -1;
            opcode = is.rawByte();
            Optional<? extends Op> optInsn = (switch (opcode) {
                case OP_PREFIX_FC, OP_PREFIX_FD, OP_PREFIX_FE -> Ops.forOpcode(prefix = opcode, opcode = is.u32());
                default -> Ops.forOpcode(opcode);
            });
            if (optInsn.isEmpty()) {
                throw invalidOpcode(prefix, opcode);
            }
            Op insn = optInsn.get();
            switch (insn.kind()) {
                case BLOCK -> {
                    Op.Block blockType = (Op.Block) insn;
                    int peek = is.peekRawByte();
                    if (peek == 0x40) {
                        is.rawByte();
                        // 𝜖
                        ev.visit(blockType);
                    } else {
                        // actually s33
                        long val = is.s64();
                        if (val < 0) {
                            // it's actually a type value
                            ev.visit(blockType, ValType.forByteValue(peek));
                        } else {
                            ev.visit(blockType, (int) val);
                        }
                    }
                    switch (blockType) {
                        case block, loop, if_ -> nesting++;
                    }
                }
                case CONST_F32 -> ev.visit((Op.ConstF32) insn, is.f32());
                case CONST_F64 -> ev.visit((Op.ConstF64) insn, is.f64());
                case CONST_I32 -> ev.visit((Op.ConstI32) insn, is.u32());
                case CONST_I64 -> ev.visit((Op.ConstI64) insn, is.u64());
                case CONST_I128 -> {
                    I128 i128 = is.u128();
                    ev.visit((Op.ConstI128) insn, i128.low(), i128.high());
                }
                case DATA -> ev.visit((Op.Data) insn, is.u32());
                case ELEMENT -> ev.visit((Op.Element) insn, is.u32());
                case ELEMENT_AND_TABLE -> ev.visit((Op.ElementAndTable) insn, is.u32(), is.u32());
                case EXCEPTION ->  ev.visit((Op.Exception) insn, is.u32());
                case FUNC -> ev.visit((Op.Func) insn, is.u32());
                case GLOBAL -> ev.visit((Op.Global) insn, is.u32());
                case BRANCH ->  ev.visit((Op.Branch) insn, is.u32());
                case LANE ->  ev.visit((Op.Lane) insn, is.u32());
                case LOCAL -> ev.visit((Op.Local) insn, is.u32());
                case MEMORY -> ev.visit((Op.Memory) insn, is.u32());
                case MEMORY_ACCESS -> {
                    int peek = is.peekRawByte();
                    int align = 1 << is.u32();
                    int memory = ((peek & 0x40) != 0) ? is.u32() : 0;
                    int offset = is.u32();
                    ev.visit((Op.MemoryAccess) insn, memory, align, offset);
                }
                case MEMORY_ACCESS_LANE -> {
                    int peek = is.peekRawByte();
                    int align = 1 << is.u32();
                    int memory = ((peek & 0x40) != 0) ? is.u32() : 0;
                    int offset = is.u32();
                    int index = is.u32();
                    ev.visit((Op.MemoryAccessLane) insn, memory, align, offset, index);
                }
                case MEMORY_AND_DATA -> {
                    int dataIdx = is.u32();
                    int memIdx = is.u32();
                    ev.visit((Op.MemoryAndData) insn, dataIdx, memIdx);
                }
                case MEMORY_AND_MEMORY -> ev.visit((Op.MemoryAndMemory) insn, is.u32(), is.u32());
                case MULTI_BRANCH -> {
                    int[] targets = is.u32Vec();
                    int defaultTarget = is.u32();
                    ev.visit((Op.MultiBranch) insn, defaultTarget, targets);
                }
                case REF_TYPED -> ev.visit((Op.RefTyped) insn, is.refType());
                case SIMPLE -> {
                    Op.Simple simple = (Op.Simple) insn;
                    ev.visit(simple);
                    if (simple == Ops.end) {
                        if (nesting-- == 0) {
                            ev.visitEnd();
                            return;
                        }
                    }
                }
                case TABLE -> ev.visit((Op.Table) insn, is.u32());
                case TABLE_AND_FUNC_TYPE -> {
                    int typeIdx = is.u32();
                    int tableIdx = is.u32();
                    ev.visit((Op.TableAndFuncType) insn, tableIdx, typeIdx);
                }
                case TABLE_AND_TABLE -> ev.visit((Op.TableAndTable) insn, is.u32(), is.u32());
                case TAG -> ev.visit((Op.Tag) insn, is.u32());
                case TYPES -> ev.visit((Op.Types) insn, is.typeVec().toArray(ValType[]::new));
            }
        }
    }

    private static IOException invalidOpcode(int prefix, int opcode) {
        return prefix == -1 ?
            new IOException(String.format("Invalid opcode 0x%02x", Integer.valueOf(opcode)))
            : new IOException(String.format("Invalid opcode 0x%02x 0x%02x", Integer.valueOf(prefix), Integer.valueOf(opcode)));
    }

    @Override
    public void close() throws IOException {
        is.close();
    }
}

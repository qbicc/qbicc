package org.qbicc.machine.file.wasm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.qbicc.machine.file.wasm.model.BranchTarget;
import org.qbicc.machine.file.wasm.model.InsnSeq;
import org.qbicc.machine.file.wasm.model.Resolver;
import org.qbicc.machine.file.wasm.stream.WasmInputStream;

/**
 * A raw operation of some kind.
 */
@SuppressWarnings("SpellCheckingInspection")
public sealed interface Op permits Op.AtomicMemoryAccess,
                                   Op.Block,
                                   Op.ConstF32,
                                   Op.ConstF64,
                                   Op.ConstI32,
                                   Op.ConstI64,
                                   Op.ConstV128,
                                   Op.Data,
                                   Op.MemoryAndData,
                                   Op.Element,
                                   Op.ElementAndTable,
                                   Op.Exception,
                                   Op.Func,
                                   Op.Global,
                                   Op.Local,
                                   Op.Branch,
                                   Op.Lane,
                                   Op.Memory,
                                   Op.MemoryToMemory,
                                   Op.MemoryAccess,
                                   Op.MemoryAccessLane,
                                   Op.MultiBranch,
                                   Op.Prefix,
                                   Op.RefTyped,
                                   Op.Simple,
                                   Op.Table,
                                   Op.TableToTable,
                                   Op.TableAndFuncType,
                                   Op.Tag,
                                   Op.Types
{
    int opcode();

    Optional<Prefix> prefix();

    Kind kind();

    default Set<Flag> flags() {
        return Set.of();
    }

    Optional<? extends Op> optional();

    void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException;

    void skip(WasmInputStream is) throws IOException;

    enum Kind {
        ATOMIC_MEMORY_ACCESS(Op.AtomicMemoryAccess.class),
        BLOCK(Op.Block.class),
        BRANCH(Op.Branch.class),
        CONST_F32(Op.ConstF32.class),
        CONST_F64(Op.ConstF64.class),
        CONST_I32(Op.ConstI32.class),
        CONST_I64(Op.ConstI64.class),
        CONST_V128(Op.ConstV128.class),
        DATA(Op.Data.class),
        ELEMENT_AND_TABLE(Op.ElementAndTable.class),
        ELEMENT(Op.Element.class),
        EXCEPTION(Op.Exception.class),
        FUNC(Op.Func.class),
        GLOBAL(Op.Global.class),
        LANE(Op.Lane.class),
        LOCAL(Op.Local.class),
        MEMORY(Op.Memory.class),
        MEMORY_ACCESS(Op.MemoryAccess.class),
        MEMORY_ACCESS_LANE(Op.MemoryAccessLane.class),
        MEMORY_AND_DATA(Op.MemoryAndData.class),
        MEMORY_AND_MEMORY(MemoryToMemory.class),
        MULTI_BRANCH(Op.MultiBranch.class),
        PREFIX(Op.Prefix.class),
        REF_TYPED(Op.RefTyped.class),
        SIMPLE(Op.Simple.class),
        TABLE(Op.Table.class),
        TABLE_AND_FUNC_TYPE(Op.TableAndFuncType.class),
        TABLE_AND_TABLE(TableToTable.class),
        TAG(Op.Tag.class),
        TYPES(Op.Types.class),
        ;

        private static final List<Kind> kinds = List.of(values());
        private final Class<? extends Op> opClass;
        private final List<Op> ops;

        Kind(Class<? extends Op> opClass) {
            this.opClass = opClass;
            ops = List.of(opClass.getEnumConstants());
        }

        public Class<? extends Op> opClass() {
            return opClass;
        }

        public List<Op> ops() {
            return ops;
        }

        public static List<Kind> all() {
            return kinds;
        }
    }

    enum Flag {
    }

    enum Block implements Op {
        block("block", Opcodes.OP_BLOCK),
        loop("loop", Opcodes.OP_LOOP),
        if_("if", Opcodes.OP_IF),
        try_("try", Opcodes.OP_TRY),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Block> optional;

        Block(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Block(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Block(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.BLOCK;
        }

        @Override
        public Optional<Block> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            FuncType type;
            int b = is.peekRawByte();
            if (b == 0x40) {
                type = FuncType.EMPTY;
                is.rawByte();
            } else if (ValType.isValidByteValue(b)) {
                type = ValType.forByteValue(b).asFuncTypeReturning();
                is.rawByte();
            } else {
                type = resolver.resolveFuncType(is.u32());
            }
            seq.add(this, type, insn -> insn.body().readFrom(is, new Resolver.Delegating() {
                @Override
                public Resolver getDelegate() {
                    return resolver;
                }

                @Override
                public BranchTarget resolveBranchTarget(int index) {
                    return index == 0 ? insn : getDelegate().resolveBranchTarget(index - 1);
                }
            }));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            int b = is.peekRawByte();
            if (b == 0x40) {
                is.rawByte();
            } else if (ValType.isValidByteValue(b)) {
                is.rawByte();
            } else {
                is.u32();
            }
            InsnSeq.skip(is);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Branch implements Op {
        // label index
        br("br", Opcodes.OP_BR),
        br_if("br_if", Opcodes.OP_BR_IF),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Branch> optional;

        Branch(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Branch(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Branch(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.BRANCH;
        }

        @Override
        public Optional<Branch> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveBranchTarget(is.u32()));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum ConstF32 implements Op {
        f32_const("f32.const", Opcodes.OP_F32_CONST),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<ConstF32> optional;

        ConstF32(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        ConstF32(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        ConstF32(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.CONST_F32;
        }

        @Override
        public Optional<ConstF32> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, is.f32());
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.f32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum ConstF64 implements Op {
        f64_const("f64.const", Opcodes.OP_F64_CONST),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<ConstF64> optional;

        ConstF64(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        ConstF64(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        ConstF64(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.CONST_F64;
        }

        @Override
        public Optional<ConstF64> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, is.f64());
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.f64();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum ConstI32 implements Op {
        i32_const("i32.const", Opcodes.OP_I32_CONST),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<ConstI32> optional;

        ConstI32(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        ConstI32(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        ConstI32(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.CONST_I32;
        }

        @Override
        public Optional<ConstI32> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, is.s32());
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.s32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum ConstI64 implements Op {
        i64_const("i64.const", Opcodes.OP_I64_CONST),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<ConstI64> optional;

        ConstI64(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        ConstI64(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        ConstI64(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.CONST_I64;
        }

        @Override
        public Optional<ConstI64> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, is.s64());
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.s64();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum ConstV128 implements Op {
        v128_const("v128.const", Prefix.fd, Opcodes.OP_FD_V128_CONST),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<ConstV128> optional;

        ConstV128(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        ConstV128(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        ConstV128(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.CONST_V128;
        }

        @Override
        public Optional<ConstV128> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, is.rawLong(), is.rawLong());
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.skip(8);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Data implements Op {
        // data index
        data_drop("data.drop", Prefix.fc, Opcodes.OP_FC_DATA_DROP),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Data> optional;

        Data(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Data(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Data(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.DATA;
        }

        @Override
        public Optional<Data> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveSegment(is.u32()));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Element implements Op {
        // element index
        elem_drop("elem.drop", Prefix.fc, Opcodes.OP_FC_ELEM_DROP),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Element> optional;

        Element(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Element(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Element(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.ELEMENT;
        }

        @Override
        public Optional<Element> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveElement(is.u32()));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum ElementAndTable implements Op {
        // elemidx, tableidx
        table_init("table.init", Prefix.fc, Opcodes.OP_FC_TABLE_INIT),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<ElementAndTable> optional;

        ElementAndTable(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        ElementAndTable(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        ElementAndTable(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.ELEMENT_AND_TABLE;
        }

        @Override
        public Optional<ElementAndTable> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveElement(is.u32()), resolver.resolveTable(is.u32()));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Exception implements Op {
        // label index
        delegate("delegate", Opcodes.OP_DELEGATE),
        rethrow("rethrow", Opcodes.OP_RETHROW),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Exception> optional;

        Exception(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Exception(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Exception(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.EXCEPTION;
        }

        @Override
        public Optional<Exception> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveBranchTarget(is.u32()));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Func implements Op {
        // function index
        call("call", Opcodes.OP_CALL),
        ref_func("ref.func", Opcodes.OP_REF_FUNC),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Func> optional;

        Func(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Func(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Func(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.FUNC;
        }

        @Override
        public Optional<Func> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveFunc(is.u32()));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Global implements Op {
        // global index
        global_get("global.get", Opcodes.OP_GLOBAL_GET),
        global_set("global.set", Opcodes.OP_GLOBAL_SET),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Global> optional;

        Global(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Global(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Global(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.GLOBAL;
        }

        @Override
        public Optional<Global> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveGlobal(is.u32()));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Lane implements Op {
        // vector lane index
        i8x16_extract_lane_s("i8x16.extract_lane_s", Prefix.fd, Opcodes.OP_FD_I8X16_EXTRACT_LANE_S),
        i8x16_extract_lane_u("i8x16.extract_lane_u", Prefix.fd, Opcodes.OP_FD_I8X16_EXTRACT_LANE_S),
        i8x16_replace_lane("i8x16.replace_lane", Prefix.fd, Opcodes.OP_FD_I8X16_REPLACE_LANE),
        i8x16_shuffle("i8x16.shuffle", Prefix.fd, Opcodes.OP_FD_I8X16_SHUFFLE),
        i16x8_extract_lane_s("i16x8.extract_lane_s", Prefix.fd, Opcodes.OP_FD_I16X8_EXTRACT_LANE_S),
        i16x8_extract_lane_u("i16x8.extract_lane_u", Prefix.fd, Opcodes.OP_FD_I16X8_EXTRACT_LANE_U),
        i16x8_replace_lane("i16x8.replace_lane", Prefix.fd, Opcodes.OP_FD_I16X8_REPLACE_LANE),
        i32x4_extract_lane("i32x4.extract_lane", Prefix.fd, Opcodes.OP_FD_I32X4_EXTRACT_LANE),
        i32x4_replace_lane("i32x4.replace_lane", Prefix.fd, Opcodes.OP_FD_I32X4_REPLACE_LANE),
        i64x2_extract_lane("i64x2.extract_lane", Prefix.fd, Opcodes.OP_FD_I64X2_EXTRACT_LANE),
        i64x2_replace_lane("i64x2.replace_lane", Prefix.fd, Opcodes.OP_FD_I64X2_REPLACE_LANE),
        f32x4_extract_lane("f32x4.extract_lane", Prefix.fd, Opcodes.OP_FD_F32X4_EXTRACT_LANE),
        f32x4_replace_lane("f32x4.replace_lane", Prefix.fd, Opcodes.OP_FD_F32X4_REPLACE_LANE),
        f64x2_extract_lane("f64x2.extract_lane", Prefix.fd, Opcodes.OP_FD_F64X2_EXTRACT_LANE),
        f64x2_replace_lane("f64x2.replace_lane", Prefix.fd, Opcodes.OP_FD_F64X2_REPLACE_LANE),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Lane> optional;

        Lane(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Lane(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Lane(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.LANE;
        }

        @Override
        public Optional<Lane> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, is.u32());
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Local implements Op {
        // local index
        local_get("local.get", Opcodes.OP_LOCAL_GET),
        local_set("local.set", Opcodes.OP_LOCAL_SET),
        local_tee("local.tee", Opcodes.OP_LOCAL_TEE),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Local> optional;

        Local(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Local(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Local(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.LOCAL;
        }

        @Override
        public Optional<Local> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveLocal(is.u32()));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Memory implements Op {
        // memory index
        memory_fill("memory.fill", Prefix.fc, Opcodes.OP_FC_MEMORY_FILL),
        memory_grow("memory.grow", Opcodes.OP_MEMORY_GROW),
        memory_size("memory.size", Opcodes.OP_MEMORY_SIZE),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Memory> optional;

        Memory(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Memory(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Memory(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.MEMORY;
        }

        @Override
        public Optional<Memory> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveMemory(is.u32()));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum AtomicMemoryAccess implements Op {
        i32_atomic_load("i32.atomic.load", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_LOAD, 4),
        i32_atomic_load16_u("i32.atomic.load16_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_LOAD16_U, 2),
        i32_atomic_load8_u("i32.atomic.load8_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_LOAD8_U, 1),
        i32_atomic_rmw16_add_u("i32.atomic.rmw16.add_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW16_ADD_U, 2),
        i32_atomic_rmw16_and_u("i32.atomic.rmw16.and_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW16_AND_U, 2),
        i32_atomic_rmw16_cmpxchg_u("i32.atomic.rmw16.cmpxchg_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW16_CMPXCHG_U, 2),
        i32_atomic_rmw16_or_u("i32.atomic.rmw16.or_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW16_OR_U, 2),
        i32_atomic_rmw16_sub_u("i32.atomic.rmw16.sub_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW16_SUB_U, 2),
        i32_atomic_rmw16_xchg_u("i32.atomic.rmw16.xchg_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW16_XCHG_U, 2),
        i32_atomic_rmw16_xor_u("i32.atomic.rmw16.xor_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW16_XOR_U, 2),
        i32_atomic_rmw8_add_u("i32.atomic.rmw8.add_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW8_ADD_U, 1),
        i32_atomic_rmw8_and_u("i32.atomic.rmw8.and_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW8_AND_U, 1),
        i32_atomic_rmw8_cmpxchg_u("i32.atomic.rmw8.cmpxchg_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW8_CMPXCHG_U, 1),
        i32_atomic_rmw8_or_u("i32.atomic.rmw8.or_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW8_OR_U, 1),
        i32_atomic_rmw8_sub_u("i32.atomic.rmw8.sub_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW8_SUB_U, 1),
        i32_atomic_rmw8_xchg_u("i32.atomic.rmw8.xchg_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW8_XCHG_U, 1),
        i32_atomic_rmw8_xor_u("i32.atomic.rmw8.xor_u", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW8_XOR_U, 1),
        i32_atomic_rmw_add("i32.atomic.rmw.add", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW_ADD, 4),
        i32_atomic_rmw_and("i32.atomic.rmw.and", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW_AND, 4),
        i32_atomic_rmw_cmpxchg("i32.atomic.rmw.cmpxchg", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW_CMPXCHG, 4),
        i32_atomic_rmw_or("i32.atomic.rmw.or", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW_OR, 4),
        i32_atomic_rmw_sub("i32.atomic.rmw.sub", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW_SUB, 4),
        i32_atomic_rmw_xchg("i32.atomic.rmw.xchg", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW_XCHG, 4),
        i32_atomic_rmw_xor("i32.atomic.rmw.xor", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_RMW_XOR, 4),
        i32_atomic_store("i32.atomic.store", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_STORE, 4),
        i32_atomic_store16("i32.atomic.store16", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_STORE16, 2),
        i32_atomic_store8("i32.atomic.store8", Prefix.fe, Opcodes.OP_FE_I32_ATOMIC_STORE8, 1),
        i64_atomic_load("i64.atomic.load", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_LOAD, 8),
        i64_atomic_load16_u("i64.atomic.load16_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_LOAD16_U, 2),
        i64_atomic_load32_u("i64.atomic.load32_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_LOAD32_U, 4),
        i64_atomic_load8_u("i64.atomic.load8_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_LOAD8_U, 1),
        i64_atomic_rmw16_add_u("i64.atomic.rmw16.add_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW16_ADD_U, 2),
        i64_atomic_rmw16_and_u("i64.atomic.rmw16.and_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW16_AND_U, 2),
        i64_atomic_rmw16_cmpxchg_u("i64.atomic.rmw16.cmpxchg_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW16_CMPXCHG_U, 2),
        i64_atomic_rmw16_or_u("i64.atomic.rmw16.or_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW16_OR_U, 2),
        i64_atomic_rmw16_sub_u("i64.atomic.rmw16.sub_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW16_SUB_U, 2),
        i64_atomic_rmw16_xchg_u("i64.atomic.rmw16.xchg_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW16_XCHG_U, 2),
        i64_atomic_rmw16_xor_u("i64.atomic.rmw16.xor_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW16_XOR_U, 2),
        i64_atomic_rmw32_add_u("i64.atomic.rmw32.add_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW32_ADD_U, 4),
        i64_atomic_rmw32_and_u("i64.atomic.rmw32.and_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW32_AND_U, 4),
        i64_atomic_rmw32_cmpxchg_u("i64.atomic.rmw32.cmpxchg_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW32_CMPXCHG_U, 4),
        i64_atomic_rmw32_or_u("i64.atomic.rmw32.or_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW32_OR_U, 4),
        i64_atomic_rmw32_sub_u("i64.atomic.rmw32.sub_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW32_SUB_U, 4),
        i64_atomic_rmw32_xchg_u("i64.atomic.rmw32.xchg_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW32_XCHG_U, 4),
        i64_atomic_rmw32_xor_u("i64.atomic.rmw32.xor_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW32_XOR_U, 4),
        i64_atomic_rmw8_add_u("i64.atomic.rmw8.add_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW8_ADD_U, 1),
        i64_atomic_rmw8_and_u("i64.atomic.rmw8.and_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW8_AND_U, 1),
        i64_atomic_rmw8_cmpxchg_u("i64.atomic.rmw8.cmpxchg_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW8_CMPXCHG_U, 1),
        i64_atomic_rmw8_or_u("i64.atomic.rmw8.or_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW8_OR_U, 1),
        i64_atomic_rmw8_sub_u("i64.atomic.rmw8.sub_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW8_SUB_U, 1),
        i64_atomic_rmw8_xchg_u("i64.atomic.rmw8.xchg_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW8_XCHG_U, 1),
        i64_atomic_rmw8_xor_u("i64.atomic.rmw8.xor_u", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW8_XOR_U, 1),
        i64_atomic_rmw_add("i64.atomic.rmw.add", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW_ADD, 8),
        i64_atomic_rmw_and("i64.atomic.rmw.and", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW_AND, 8),
        i64_atomic_rmw_cmpxchg("i64.atomic.rmw.cmpxchg", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW_CMPXCHG, 8),
        i64_atomic_rmw_or("i64.atomic.rmw.or", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW_OR, 8),
        i64_atomic_rmw_sub("i64.atomic.rmw.sub", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW_SUB, 8),
        i64_atomic_rmw_xchg("i64.atomic.rmw.xchg", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW_XCHG, 8),
        i64_atomic_rmw_xor("i64.atomic.rmw.xor", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_RMW_XOR, 8),
        i64_atomic_store("i64.atomic.store", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_STORE, 8),
        i64_atomic_store16("i64.atomic.store16", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_STORE16, 2),
        i64_atomic_store32("i64.atomic.store32", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_STORE32, 4),
        i64_atomic_store8("i64.atomic.store8", Prefix.fe, Opcodes.OP_FE_I64_ATOMIC_STORE8, 1),
        memory_atomic_notify("memory.atomic.notify", Prefix.fe, Opcodes.OP_FE_MEMORY_ATOMIC_NOTIFY, 4),
        memory_atomic_wait32("memory.atomic.wait32", Prefix.fe, Opcodes.OP_FE_MEMORY_ATOMIC_WAIT32, 4),
        memory_atomic_wait64("memory.atomic.wait64", Prefix.fe, Opcodes.OP_FE_MEMORY_ATOMIC_WAIT64, 8),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final int alignment;
        private final Optional<AtomicMemoryAccess> optional;

        AtomicMemoryAccess(String name, Optional<Prefix> prefix, int opcode, int alignment) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.alignment = alignment;
            this.optional = Optional.of(this);
        }

        AtomicMemoryAccess(String name, Prefix prefix, int opcode, int alignment) {
            this(name, prefix.optional(), opcode, alignment);
        }

        AtomicMemoryAccess(String name, int opcode, int alignment) {
            this(name, Optional.empty(), opcode, alignment);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        public int alignment() {
            return alignment;
        }

        @Override
        public Kind kind() {
            return Kind.ATOMIC_MEMORY_ACCESS;
        }

        @Override
        public Optional<AtomicMemoryAccess> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveMemory(is.u32()), is.u32());
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum MemoryAccess implements Op {
        f32_load("f32.load", Opcodes.OP_F32_LOAD),
        f32_store("f32.store", Opcodes.OP_F32_STORE),
        f64_load("f64.load", Opcodes.OP_F64_LOAD),
        f64_store("f64.store", Opcodes.OP_F64_STORE),
        i32_load("i32.load", Opcodes.OP_I32_LOAD),
        i32_load16_s("i32.load16_s", Opcodes.OP_I32_LOAD16_S),
        i32_load16_u("i32.load16_u", Opcodes.OP_I32_LOAD16_U),
        i32_load8_s("i32.load8_s", Opcodes.OP_I32_LOAD8_S),
        i32_load8_u("i32.load8_u", Opcodes.OP_I32_LOAD8_U),
        i32_store("i32.store", Opcodes.OP_I32_STORE),
        i32_store16("i32.store16", Opcodes.OP_I32_STORE16),
        i32_store8("i32.store8", Opcodes.OP_I32_STORE8),
        i64_load("i64.load", Opcodes.OP_I64_LOAD),
        i64_load16_s("i64.load16_s", Opcodes.OP_I64_LOAD16_S),
        i64_load16_u("i64.load16_u", Opcodes.OP_I64_LOAD16_U),
        i64_load32_s("i64.load32_s", Opcodes.OP_I64_LOAD32_S),
        i64_load32_u("i64.load32_u", Opcodes.OP_I64_LOAD32_U),
        i64_load8_s("i64.load8_s", Opcodes.OP_I64_LOAD8_S),
        i64_load8_u("i64.load8_u", Opcodes.OP_I64_LOAD8_U),
        i64_store("i64.store", Opcodes.OP_I64_STORE),
        i64_store16("i64.store16", Opcodes.OP_I64_STORE16),
        i64_store32("i64.store32", Opcodes.OP_I64_STORE32),
        i64_store8("i64.store8", Opcodes.OP_I64_STORE8),
        v128_load("v128.load", Prefix.fd, Opcodes.OP_FD_V128_LOAD),
        v128_load16_splat("v128.load16_splat", Prefix.fd, Opcodes.OP_FD_V128_LOAD16_SPLAT),
        v128_load16x4_s("v128.load16x4_s", Prefix.fd, Opcodes.OP_FD_V128_LOAD16X4_S),
        v128_load16x4_u("v128.load16x4_u", Prefix.fd, Opcodes.OP_FD_V128_LOAD16X4_U),
        v128_load32_splat("v128.load32_splat", Prefix.fd, Opcodes.OP_FD_V128_LOAD32_SPLAT),
        v128_load32_zero("v128.load32_zero", Prefix.fd, Opcodes.OP_FD_V128_LOAD32_ZERO),
        v128_load32x2_s("v128.load32x2_s", Prefix.fd, Opcodes.OP_FD_V128_LOAD32X2_S),
        v128_load32x2_u("v128.load32x2_u", Prefix.fd, Opcodes.OP_FD_V128_LOAD32X2_U),
        v128_load64_splat("v128.load64_splat", Prefix.fd, Opcodes.OP_FD_V128_LOAD64_SPLAT),
        v128_load64_zero("v128.load64_zero", Prefix.fd, Opcodes.OP_FD_V128_LOAD64_ZERO),
        v128_load8_splat("v128.load8_splat", Prefix.fd, Opcodes.OP_FD_V128_LOAD8_SPLAT),
        v128_load8x8_s("v128.load8x8_s", Prefix.fd, Opcodes.OP_FD_V128_LOAD8X8_S),
        v128_load8x8_u("v128.load8x8_u", Prefix.fd, Opcodes.OP_FD_V128_LOAD8X8_U),
        v128_store("v128.store", Prefix.fd, Opcodes.OP_FD_V128_STORE),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<MemoryAccess> optional;

        MemoryAccess(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        MemoryAccess(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        MemoryAccess(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.MEMORY_ACCESS;
        }

        @Override
        public Optional<MemoryAccess> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            boolean hasMem = (is.peekRawByte() & 0x40) != 0;
            int alignShift = is.u32() & 0b111111;
            int memIdx = hasMem ? is.u32() : 0;
            int offset = is.u32();
            seq.add(this, resolver.resolveMemory(memIdx), offset, 1 << alignShift);
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            boolean hasMem = (is.peekRawByte() & 0x40) != 0;
            is.u32();
            if (hasMem) is.u32();
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum MemoryAccessLane implements Op {
        v128_load8_lane("v128.load8_lane", Prefix.fd, Opcodes.OP_FD_V128_LOAD8_LANE),
        v128_load16_lane("v128.load16_lane", Prefix.fd, Opcodes.OP_FD_V128_LOAD16_LANE),
        v128_load32_lane("v128.load32_lane", Prefix.fd, Opcodes.OP_FD_V128_LOAD32_LANE),
        v128_load64_lane("v128.load64_lane", Prefix.fd, Opcodes.OP_FD_V128_LOAD64_LANE),
        v128_store8_lane("v128.store8_lane", Prefix.fd, Opcodes.OP_FD_V128_STORE8_LANE),
        v128_store16_lane("v128.store16_lane", Prefix.fd, Opcodes.OP_FD_V128_STORE16_LANE),
        v128_store32_lane("v128.store32_lane", Prefix.fd, Opcodes.OP_FD_V128_STORE32_LANE),
        v128_store64_lane("v128.store64_lane", Prefix.fd, Opcodes.OP_FD_V128_STORE64_LANE),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<MemoryAccessLane> optional;

        MemoryAccessLane(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        MemoryAccessLane(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        MemoryAccessLane(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.MEMORY_ACCESS_LANE;
        }

        @Override
        public Optional<MemoryAccessLane> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            boolean hasMem = (is.peekRawByte() & 0x40) != 0;
            int alignShift = is.u32() & 0b111111;
            int memIdx = hasMem ? is.u32() : 0;
            int offset = is.u32();
            int lane = is.u32();
            seq.add(this, resolver.resolveMemory(memIdx), offset, 1 << alignShift, lane);
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            boolean hasMem = (is.peekRawByte() & 0x40) != 0;
            is.u32();
            if (hasMem) is.u32();
            is.u32();
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum MemoryAndData implements Op {
        // dataidx, memidx
        memory_init("memory.init", Prefix.fc, Opcodes.OP_FC_MEMORY_INIT),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<MemoryAndData> optional;

        MemoryAndData(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        MemoryAndData(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        MemoryAndData(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.MEMORY_AND_DATA;
        }

        @Override
        public Optional<MemoryAndData> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            int segIdx = is.u32();
            int memIdx = is.u32();
            seq.add(this, resolver.resolveMemory(memIdx), resolver.resolveSegment(segIdx));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum MemoryToMemory implements Op {
        // memidx, memidx
        memory_copy("memory.copy", Prefix.fc, Opcodes.OP_FC_MEMORY_COPY),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<MemoryToMemory> optional;

        MemoryToMemory(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        MemoryToMemory(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        MemoryToMemory(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.MEMORY_AND_MEMORY;
        }

        @Override
        public Optional<MemoryToMemory> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveMemory(is.u32()), resolver.resolveMemory(is.u32()));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum MultiBranch implements Op {
        br_table("br_table", Opcodes.OP_BR_TABLE);

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<MultiBranch> optional;

        MultiBranch(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        MultiBranch(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        MultiBranch(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.MULTI_BRANCH;
        }

        @Override
        public Optional<MultiBranch> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            int cnt = is.u32();
            List<BranchTarget> targets = cnt == 0 ? List.of() : new ArrayList<>(cnt);
            for (int i = 0; i < cnt; i ++) {
                targets.add(resolver.resolveBranchTarget(is.u32()));
            }
            BranchTarget defTarget = resolver.resolveBranchTarget(is.u32());
            seq.add(this, List.copyOf(targets), defTarget);
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            int cnt = is.u32();
            for (int i = 0; i < cnt; i ++) {
                is.u32();
            }
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Prefix implements Op {
        fc(Opcodes.OP_PREFIX_FC),
        fd(Opcodes.OP_PREFIX_FD),
        fe(Opcodes.OP_PREFIX_FE),
        ;
        private final int opcode;
        private final Optional<Prefix> optional;

        Prefix(int opcode) {
            this.opcode = opcode;
            optional = Optional.of(this);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return Optional.empty();
        }

        @Override
        public Kind kind() {
            return Kind.PREFIX;
        }

        @Override
        public Optional<Prefix> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) {
            // no arguments
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            // no arguments
        }

        @Override
        public String toString() {
            return "0x" + name();
        }
    }

    enum RefTyped implements Op {
        ref_null("ref.null", Opcodes.OP_REF_NULL),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<RefTyped> optional;

        RefTyped(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        RefTyped(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        RefTyped(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.REF_TYPED;
        }

        @Override
        public Optional<RefTyped> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, is.refType());
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.refType();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Instructions which take no arguments.
     * For code generation, use the constants in {@link Ops} instead.
     */
    enum Simple implements Op {
        catch_all("catch_all", Opcodes.OP_CATCH),
        drop("drop", Opcodes.OP_DROP),
        else_("else", Opcodes.OP_ELSE),
        end("end", Opcodes.OP_END),
        f32_abs("f32.abs", Opcodes.OP_F32_ABS),
        f32_add("f32.add", Opcodes.OP_F32_ADD),
        f32_ceil("f32.ceil", Opcodes.OP_F32_CEIL),
        f32_convert_i32_s("f32.convert_i32_s", Opcodes.OP_F32_CONVERT_I32_S),
        f32_convert_i32_u("f32.convert_i32_u", Opcodes.OP_F32_CONVERT_I32_U),
        f32_convert_i64_s("f32.convert_i64_s", Opcodes.OP_F32_CONVERT_I64_S),
        f32_convert_i64_u("f32.convert_i64_u", Opcodes.OP_F32_CONVERT_I64_U),
        f32_copysign("f32.copysign", Opcodes.OP_F32_COPYSIGN),
        f32_demote_f64("f32.demote_f64", Opcodes.OP_F32_DEMOTE_F64),
        f32_div("f32.div", Opcodes.OP_F32_DIV),
        f32_eq("f32.eq", Opcodes.OP_F32_EQ),
        f32_floor("f32.floor", Opcodes.OP_F32_FLOOR),
        f32_ge("f32.ge", Opcodes.OP_F32_GE),
        f32_gt("f32.gt", Opcodes.OP_F32_GT),
        f32_le("f32.le", Opcodes.OP_F32_LE),
        f32_lt("f32.lt", Opcodes.OP_F32_LT),
        f32_max("f32.max", Opcodes.OP_F32_MAX),
        f32_min("f32.min", Opcodes.OP_F32_MIN),
        f32_mul("f32.mul", Opcodes.OP_F32_MUL),
        f32_ne("f32.ne", Opcodes.OP_F32_NE),
        f32_nearest("f32.nearest", Opcodes.OP_F32_NEAREST),
        f32_neg("f32.neg", Opcodes.OP_F32_NEG),
        f32_reinterpret_i32("f32.reinterpret_i32", Opcodes.OP_F32_REINTERPRET_I32),
        f32_sqrt("f32.sqrt", Opcodes.OP_F32_SQRT),
        f32_sub("f32.sub", Opcodes.OP_F32_SUB),
        f32_trunc("f32.trunc", Opcodes.OP_F32_TRUNC),
        f32x4_abs("f32x4.abs", Prefix.fd, Opcodes.OP_FD_F32X4_ABS),
        f32x4_add("f32x4.add", Prefix.fd, Opcodes.OP_FD_F32X4_ADD),
        f32x4_ceil("f32x4.ceil", Prefix.fd, Opcodes.OP_FD_F32X4_CEIL),
        f32x4_convert_i32x4_s("f32x4.convert_i32x4_s", Prefix.fd, Opcodes.OP_FD_F32X4_CONVERT_I32X4_S),
        f32x4_convert_i32x4_u("f32x4.convert_i32x4_u", Prefix.fd, Opcodes.OP_FD_F32X4_CONVERT_I32X4_U),
        f32x4_demote_f64x2_zero("f32x4.demote_f64x2_zero", Prefix.fd, Opcodes.OP_FD_F32X4_DEMOTE_F64X2_ZERO),
        f32x4_div("f32x4.div", Prefix.fd, Opcodes.OP_FD_F32X4_DIV),
        f32x4_eq("f32x4.eq", Prefix.fd, Opcodes.OP_FD_F32X4_EQ),
        f32x4_floor("f32x4.floor", Prefix.fd, Opcodes.OP_FD_F32X4_FLOOR),
        f32x4_ge("f32x4.ge", Prefix.fd, Opcodes.OP_FD_F32X4_GE),
        f32x4_gt("f32x4.gt", Prefix.fd, Opcodes.OP_FD_F32X4_GT),
        f32x4_le("f32x4.le", Prefix.fd, Opcodes.OP_FD_F32X4_LE),
        f32x4_lt("f32x4.lt", Prefix.fd, Opcodes.OP_FD_F32X4_LT),
        f32x4_max("f32x4.max", Prefix.fd, Opcodes.OP_FD_F32X4_MAX),
        f32x4_min("f32x4.min", Prefix.fd, Opcodes.OP_FD_F32X4_MIN),
        f32x4_mul("f32x4.mul", Prefix.fd, Opcodes.OP_FD_F32X4_MUL),
        f32x4_ne("f32x4.ne", Prefix.fd, Opcodes.OP_FD_F32X4_NE),
        f32x4_nearest("f32x4.nearest", Prefix.fd, Opcodes.OP_FD_F32X4_NEAREST),
        f32x4_neg("f32x4.neg", Prefix.fd, Opcodes.OP_FD_F32X4_NEG),
        f32x4_pmax("f32x4.pmax", Prefix.fd, Opcodes.OP_FD_F32X4_PMAX),
        f32x4_pmin("f32x4.pmin", Prefix.fd, Opcodes.OP_FD_F32X4_PMIN),
        f32x4_splat("f32x4.splat", Prefix.fd, Opcodes.OP_FD_F32X4_SPLAT),
        f32x4_sqrt("f32x4.sqrt", Prefix.fd, Opcodes.OP_FD_F32X4_SQRT),
        f32x4_sub("f32x4.sub", Prefix.fd, Opcodes.OP_FD_F32X4_SUB),
        f32x4_trunc("f32x4.trunc", Prefix.fd, Opcodes.OP_FD_F32X4_TRUNC),
        f64_abs("f64.abs", Opcodes.OP_F64_ABS),
        f64_add("f64.add", Opcodes.OP_F64_ADD),
        f64_ceil("f64.ceil", Opcodes.OP_F64_CEIL),
        f64_convert_i32_s("f64.convert_i32_s", Opcodes.OP_F64_CONVERT_I32_S),
        f64_convert_i32_u("f64.convert_i32_u", Opcodes.OP_F64_CONVERT_I32_U),
        f64_convert_i64_s("f64.convert_i64_s", Opcodes.OP_F64_CONVERT_I64_S),
        f64_convert_i64_u("f64.convert_i64_u", Opcodes.OP_F64_CONVERT_I64_U),
        f64_copysign("f64.copysign", Opcodes.OP_F64_COPYSIGN),
        f64_div("f64.div", Opcodes.OP_F64_DIV),
        f64_eq("f64.eq", Opcodes.OP_F64_EQ),
        f64_floor("f64.floor", Opcodes.OP_F64_FLOOR),
        f64_ge("f64.ge", Opcodes.OP_F64_GE),
        f64_gt("f64.gt", Opcodes.OP_F64_GT),
        f64_le("f64.le", Opcodes.OP_F64_LE),
        f64_lt("f64.lt", Opcodes.OP_F64_LT),
        f64_max("f64.max", Opcodes.OP_F64_MAX),
        f64_min("f64.min", Opcodes.OP_F64_MIN),
        f64_mul("f64.mul", Opcodes.OP_F64_MUL),
        f64_ne("f64.ne", Opcodes.OP_F64_NE),
        f64_nearest("f64.nearest", Opcodes.OP_F64_NEAREST),
        f64_neg("f64.neg", Opcodes.OP_F64_NEG),
        f64_promote_f32("f64.promote_f32", Opcodes.OP_F64_PROMOTE_F32),
        f64_reinterpret_i64("f64.reinterpret_i64", Opcodes.OP_F64_REINTERPRET_I64),
        f64_sqrt("f64.sqrt", Opcodes.OP_F64_SQRT),
        f64_sub("f64.sub", Opcodes.OP_F64_SUB),
        f64_trunc("f64.trunc", Opcodes.OP_F64_TRUNC),
        f64x2_abs("f64x2.abs", Prefix.fd, Opcodes.OP_FD_F64X2_ABS),
        f64x2_add("f64x2.add", Prefix.fd, Opcodes.OP_FD_F64X2_ADD),
        f64x2_ceil("f64x2.ceil", Prefix.fd, Opcodes.OP_FD_F64X2_CEIL),
        f64x2_convert_low_i32x4_s("f64x2.convert_low_i32x4_s", Prefix.fd, Opcodes.OP_FD_F64X2_CONVERT_LOW_I32X4_S),
        f64x2_convert_low_i32x4_u("f64x2.convert_low_i32x4_u", Prefix.fd, Opcodes.OP_FD_F64X2_CONVERT_LOW_I32X4_U),
        f64x2_div("f64x2.div", Prefix.fd, Opcodes.OP_FD_F64X2_DIV),
        f64x2_eq("f64x2.eq", Prefix.fd, Opcodes.OP_FD_F64X2_EQ),
        f64x2_floor("f64x2.floor", Prefix.fd, Opcodes.OP_FD_F64X2_FLOOR),
        f64x2_ge("f64x2.ge", Prefix.fd, Opcodes.OP_FD_F64X2_GE),
        f64x2_gt("f64x2.gt", Prefix.fd, Opcodes.OP_FD_F64X2_GT),
        f64x2_le("f64x2.le", Prefix.fd, Opcodes.OP_FD_F64X2_LE),
        f64x2_lt("f64x2.lt", Prefix.fd, Opcodes.OP_FD_F64X2_LT),
        f64x2_max("f64x2.max", Prefix.fd, Opcodes.OP_FD_F64X2_MAX),
        f64x2_min("f64x2.min", Prefix.fd, Opcodes.OP_FD_F64X2_MIN),
        f64x2_mul("f64x2.mul", Prefix.fd, Opcodes.OP_FD_F64X2_MUL),
        f64x2_ne("f64x2.ne", Prefix.fd, Opcodes.OP_FD_F64X2_NE),
        f64x2_nearest("f64x2.nearest", Prefix.fd, Opcodes.OP_FD_F64X2_NEAREST),
        f64x2_neg("f64x2.neg", Prefix.fd, Opcodes.OP_FD_F64X2_NEG),
        f64x2_pmax("f64x2.pmax", Prefix.fd, Opcodes.OP_FD_F64X2_PMAX),
        f64x2_pmin("f64x2.pmin", Prefix.fd, Opcodes.OP_FD_F64X2_PMIN),
        f64x2_promote_low_f32x4("f64x2.promote_low_f32x4", Prefix.fd, Opcodes.OP_FD_F64X2_PROMOTE_LOW_F32X4),
        f64x2_splat("f64x2.splat", Prefix.fd, Opcodes.OP_FD_F64X2_SPLAT),
        f64x2_sqrt("f64x2.sqrt", Prefix.fd, Opcodes.OP_FD_F64X2_SQRT),
        f64x2_sub("f64x2.sub", Prefix.fd, Opcodes.OP_FD_F64X2_SUB),
        f64x2_trunc("f64x2.trunc", Prefix.fd, Opcodes.OP_FD_F64X2_TRUNC),
        i16x8_abs("i16x8.abs", Prefix.fd, Opcodes.OP_FD_I16X8_ABS),
        i16x8_add("i16x8.add", Prefix.fd, Opcodes.OP_FD_I16X8_ADD),
        i16x8_add_sat_s("i16x8.add_sat_s", Prefix.fd, Opcodes.OP_FD_I16X8_ADD_SAT_S),
        i16x8_add_sat_u("i16x8.add_sat_u", Prefix.fd, Opcodes.OP_FD_I16X8_ADD_SAT_U),
        i16x8_all_true("i16x8.all_true", Prefix.fd, Opcodes.OP_FD_I16X8_ALL_TRUE),
        i16x8_avgr_u("i16x8.avgr_u", Prefix.fd, Opcodes.OP_FD_I16X8_AVGR_U),
        i16x8_bitmask("i16x8.bitmask", Prefix.fd, Opcodes.OP_FD_I16X8_BITMASK),
        i16x8_eq("i16x8.eq", Prefix.fd, Opcodes.OP_FD_I16X8_EQ),
        i16x8_extadd_pariwise_i8x16_s("i16x8.extadd_pariwise_i8x16_s", Prefix.fd, Opcodes.OP_FD_I16X8_EXTADD_PARIWISE_I8X16_S),
        i16x8_extadd_pariwise_i8x16_u("i16x8.extadd_pariwise_i8x16_u", Prefix.fd, Opcodes.OP_FD_I16X8_EXTADD_PARIWISE_I8X16_U),
        i16x8_extend_high_i8x16_s("i16x8.extend_high_i8x16_s", Prefix.fd, Opcodes.OP_FD_I16X8_EXTEND_HIGH_I8X16_S),
        i16x8_extend_high_i8x16_u("i16x8.extend_high_i8x16_u", Prefix.fd, Opcodes.OP_FD_I16X8_EXTEND_HIGH_I8X16_U),
        i16x8_extend_low_i8x16_s("i16x8.extend_low_i8x16_s", Prefix.fd, Opcodes.OP_FD_I16X8_EXTEND_LOW_I8X16_S),
        i16x8_extend_low_i8x16_u("i16x8.extend_low_i8x16_u", Prefix.fd, Opcodes.OP_FD_I16X8_EXTEND_LOW_I8X16_U),
        i16x8_extmul_high_i8x16_s("i16x8.extmul_high_i8x16_s", Prefix.fd, Opcodes.OP_FD_I16X8_EXTMUL_HIGH_I8X16_S),
        i16x8_extmul_high_i8x16_u("i16x8.extmul_high_i8x16_u", Prefix.fd, Opcodes.OP_FD_I16X8_EXTMUL_HIGH_I8X16_U),
        i16x8_extmul_low_i8x16_s("i16x8.extmul_low_i8x16_s", Prefix.fd, Opcodes.OP_FD_I16X8_EXTMUL_LOW_I8X16_S),
        i16x8_extmul_low_i8x16_u("i16x8.extmul_low_i8x16_u", Prefix.fd, Opcodes.OP_FD_I16X8_EXTMUL_LOW_I8X16_U),
        i16x8_ge_s("i16x8.ge_s", Prefix.fd, Opcodes.OP_FD_I16X8_GE_S),
        i16x8_ge_u("i16x8.ge_u", Prefix.fd, Opcodes.OP_FD_I16X8_GE_U),
        i16x8_gt_s("i16x8.gt_s", Prefix.fd, Opcodes.OP_FD_I16X8_GT_S),
        i16x8_gt_u("i16x8.gt_u", Prefix.fd, Opcodes.OP_FD_I16X8_GT_U),
        i16x8_le_s("i16x8.le_s", Prefix.fd, Opcodes.OP_FD_I16X8_LE_S),
        i16x8_le_u("i16x8.le_u", Prefix.fd, Opcodes.OP_FD_I16X8_LE_U),
        i16x8_lt_s("i16x8.lt_s", Prefix.fd, Opcodes.OP_FD_I16X8_LT_S),
        i16x8_lt_u("i16x8.lt_u", Prefix.fd, Opcodes.OP_FD_I16X8_LT_U),
        i16x8_max_s("i16x8.max_s", Prefix.fd, Opcodes.OP_FD_I16X8_MAX_S),
        i16x8_max_u("i16x8.max_u", Prefix.fd, Opcodes.OP_FD_I16X8_MAX_U),
        i16x8_min_s("i16x8.min_s", Prefix.fd, Opcodes.OP_FD_I16X8_MIN_S),
        i16x8_min_u("i16x8.min_u", Prefix.fd, Opcodes.OP_FD_I16X8_MIN_U),
        i16x8_mul("i16x8.mul", Prefix.fd, Opcodes.OP_FD_I16X8_MUL),
        i16x8_narrow_i32x4_s("i16x8.narrow_i32x4_s", Prefix.fd, Opcodes.OP_FD_I16X8_NARROW_I32X4_S),
        i16x8_narrow_i32x4_u("i16x8.narrow_i32x4_u", Prefix.fd, Opcodes.OP_FD_I16X8_NARROW_I32X4_U),
        i16x8_ne("i16x8.ne", Prefix.fd, Opcodes.OP_FD_I16X8_NE),
        i16x8_neg("i16x8.neg", Prefix.fd, Opcodes.OP_FD_I16X8_NEG),
        i16x8_q15mulr_sat_s("i16x8.q15mulr_sat_s", Prefix.fd, Opcodes.OP_FD_I16X8_Q15MULR_SAT_S),
        i16x8_shl("i16x8.shl", Prefix.fd, Opcodes.OP_FD_I16X8_SHL),
        i16x8_shr_s("i16x8.shr_s", Prefix.fd, Opcodes.OP_FD_I16X8_SHR_S),
        i16x8_shr_u("i16x8.shr_u", Prefix.fd, Opcodes.OP_FD_I16X8_SHR_U),
        i16x8_splat("i16x8.splat", Prefix.fd, Opcodes.OP_FD_I16X8_SPLAT),
        i16x8_sub("i16x8.sub", Prefix.fd, Opcodes.OP_FD_I16X8_SUB),
        i16x8_sub_sat_s("i16x8.sub_sat_s", Prefix.fd, Opcodes.OP_FD_I16X8_SUB_SAT_S),
        i16x8_sub_sat_u("i16x8.sub_sat_u", Prefix.fd, Opcodes.OP_FD_I16X8_SUB_SAT_U),
        i32_add("i32.add", Opcodes.OP_I32_ADD),
        i32_and("i32.and", Opcodes.OP_I32_AND),
        i32_clz("i32.clz", Opcodes.OP_I32_CLZ),
        i32_ctz("i32.ctz", Opcodes.OP_I32_CTZ),
        i32_div_s("i32.div_s", Opcodes.OP_I32_DIV_S),
        i32_div_u("i32.div_u", Opcodes.OP_I32_DIV_U),
        i32_eq("i32.eq", Opcodes.OP_I32_EQ),
        i32_eqz("i32.eqz", Opcodes.OP_I32_EQZ),
        i32_extend16_s("i32.extend16_s", Opcodes.OP_I32_EXTEND16_S),
        i32_extend8_s("i32.extend8_s", Opcodes.OP_I32_EXTEND8_S),
        i32_ge_s("i32.ge_s", Opcodes.OP_I32_GE_S),
        i32_ge_u("i32.ge_u", Opcodes.OP_I32_GE_U),
        i32_gt_s("i32.gt_s", Opcodes.OP_I32_GT_S),
        i32_gt_u("i32.gt_u", Opcodes.OP_I32_GT_U),
        i32_le_s("i32.le_s", Opcodes.OP_I32_LE_S),
        i32_le_u("i32.le_u", Opcodes.OP_I32_LE_U),
        i32_lt_s("i32.lt_s", Opcodes.OP_I32_LT_S),
        i32_lt_u("i32.lt_u", Opcodes.OP_I32_LT_U),
        i32_mul("i32.mul", Opcodes.OP_I32_MUL),
        i32_ne("i32.ne", Opcodes.OP_I32_NE),
        i32_or("i32.or", Opcodes.OP_I32_OR),
        i32_popcnt("i32.popcnt", Opcodes.OP_I32_POPCNT),
        i32_reinterpret_f32("i32.reinterpret_f32", Opcodes.OP_I32_REINTERPRET_F32),
        i32_rem_s("i32.rem_s", Opcodes.OP_I32_REM_S),
        i32_rem_u("i32.rem_u", Opcodes.OP_I32_REM_U),
        i32_rotl("i32.rotl", Opcodes.OP_I32_ROTL),
        i32_rotr("i32.rotr", Opcodes.OP_I32_ROTR),
        i32_shl("i32.shl", Opcodes.OP_I32_SHL),
        i32_shr_s("i32.shr_s", Opcodes.OP_I32_SHR_S),
        i32_shr_u("i32.shr_u", Opcodes.OP_I32_SHR_U),
        i32_sub("i32.sub", Opcodes.OP_I32_SUB),
        i32_trunc_f32_s("i32.trunc_f32_s", Opcodes.OP_I32_TRUNC_F32_S),
        i32_trunc_f32_u("i32.trunc_f32_u", Opcodes.OP_I32_TRUNC_F32_U),
        i32_trunc_f64_s("i32.trunc_f64_s", Opcodes.OP_I32_TRUNC_F64_S),
        i32_trunc_f64_u("i32.trunc_f64_u", Opcodes.OP_I32_TRUNC_F64_U),
        i32_trunc_sat_f32_s("i32.trunc_sat_f32_s", Prefix.fc, Opcodes.OP_FC_I32_TRUNC_SAT_F32_S),
        i32_trunc_sat_f32_u("i32.trunc_sat_f32_u", Prefix.fc, Opcodes.OP_FC_I32_TRUNC_SAT_F32_U),
        i32_trunc_sat_f64_s("i32.trunc_sat_f64_s", Prefix.fc, Opcodes.OP_FC_I32_TRUNC_SAT_F64_S),
        i32_trunc_sat_f64_u("i32.trunc_sat_f64_u", Prefix.fc, Opcodes.OP_FC_I32_TRUNC_SAT_F64_U),
        i32_wrap_i64("i32.wrap_i64", Opcodes.OP_I32_WRAP_I64),
        i32_xor("i32.xor", Opcodes.OP_I32_XOR),
        i32x4_abs("i32x4.abs", Prefix.fd, Opcodes.OP_FD_I32X4_ABS),
        i32x4_add("i32x4.add", Prefix.fd, Opcodes.OP_FD_I32X4_ADD),
        i32x4_all_true("i32x4.all_true", Prefix.fd, Opcodes.OP_FD_I32X4_ALL_TRUE),
        i32x4_bitmask("i32x4.bitmask", Prefix.fd, Opcodes.OP_FD_I32X4_BITMASK),
        i32x4_dot_i16x8_s("i32x4.dot_i16x8_s", Prefix.fd, Opcodes.OP_FD_I32X4_DOT_I16X8_S),
        i32x4_eq("i32x4.eq", Prefix.fd, Opcodes.OP_FD_I32X4_EQ),
        i32x4_extadd_pariwise_i16x8_s("i32x4.extadd_pariwise_i16x8_s", Prefix.fd, Opcodes.OP_FD_I32X4_EXTADD_PARIWISE_I16X8_S),
        i32x4_extadd_pariwise_i16x8_u("i32x4.extadd_pariwise_i16x8_u", Prefix.fd, Opcodes.OP_FD_I32X4_EXTADD_PARIWISE_I16X8_U),
        i32x4_extend_high_i16x8_s("i32x4.extend_high_i16x8_s", Prefix.fd, Opcodes.OP_FD_I32X4_EXTEND_HIGH_I16X8_S),
        i32x4_extend_high_i16x8_u("i32x4.extend_high_i16x8_u", Prefix.fd, Opcodes.OP_FD_I32X4_EXTEND_HIGH_I16X8_U),
        i32x4_extend_low_i16x8_s("i32x4.extend_low_i16x8_s", Prefix.fd, Opcodes.OP_FD_I32X4_EXTEND_LOW_I16X8_S),
        i32x4_extend_low_i16x8_u("i32x4.extend_low_i16x8_u", Prefix.fd, Opcodes.OP_FD_I32X4_EXTEND_LOW_I16X8_U),
        i32x4_extmul_high_i16x8_s("i32x4.extmul_high_i16x8_s", Prefix.fd, Opcodes.OP_FD_I32X4_EXTMUL_HIGH_I16X8_S),
        i32x4_extmul_high_i16x8_u("i32x4.extmul_high_i16x8_u", Prefix.fd, Opcodes.OP_FD_I32X4_EXTMUL_HIGH_I16X8_U),
        i32x4_extmul_low_i16x8_s("i32x4.extmul_low_i16x8_s", Prefix.fd, Opcodes.OP_FD_I32X4_EXTMUL_LOW_I16X8_S),
        i32x4_extmul_low_i16x8_u("i32x4.extmul_low_i16x8_u", Prefix.fd, Opcodes.OP_FD_I32X4_EXTMUL_LOW_I16X8_U),
        i32x4_ge_s("i32x4.ge_s", Prefix.fd, Opcodes.OP_FD_I32X4_GE_S),
        i32x4_ge_u("i32x4.ge_u", Prefix.fd, Opcodes.OP_FD_I32X4_GE_U),
        i32x4_gt_s("i32x4.gt_s", Prefix.fd, Opcodes.OP_FD_I32X4_GT_S),
        i32x4_gt_u("i32x4.gt_u", Prefix.fd, Opcodes.OP_FD_I32X4_GT_U),
        i32x4_le_s("i32x4.le_s", Prefix.fd, Opcodes.OP_FD_I32X4_LE_S),
        i32x4_le_u("i32x4.le_u", Prefix.fd, Opcodes.OP_FD_I32X4_LE_U),
        i32x4_lt_s("i32x4.lt_s", Prefix.fd, Opcodes.OP_FD_I32X4_LT_S),
        i32x4_lt_u("i32x4.lt_u", Prefix.fd, Opcodes.OP_FD_I32X4_LT_U),
        i32x4_max_s("i32x4.max_s", Prefix.fd, Opcodes.OP_FD_I32X4_MAX_S),
        i32x4_max_u("i32x4.max_u", Prefix.fd, Opcodes.OP_FD_I32X4_MAX_U),
        i32x4_min_s("i32x4.min_s", Prefix.fd, Opcodes.OP_FD_I32X4_MIN_S),
        i32x4_min_u("i32x4.min_u", Prefix.fd, Opcodes.OP_FD_I32X4_MIN_U),
        i32x4_mul("i32x4.mul", Prefix.fd, Opcodes.OP_FD_I32X4_MUL),
        i32x4_ne("i32x4.ne", Prefix.fd, Opcodes.OP_FD_I32X4_NE),
        i32x4_neg("i32x4.neg", Prefix.fd, Opcodes.OP_FD_I32X4_NEG),
        i32x4_shl("i32x4.shl", Prefix.fd, Opcodes.OP_FD_I32X4_SHL),
        i32x4_shr_s("i32x4.shr_s", Prefix.fd, Opcodes.OP_FD_I32X4_SHR_S),
        i32x4_shr_u("i32x4.shr_u", Prefix.fd, Opcodes.OP_FD_I32X4_SHR_U),
        i32x4_splat("i32x4.splat", Prefix.fd, Opcodes.OP_FD_I32X4_SPLAT),
        i32x4_sub("i32x4.sub", Prefix.fd, Opcodes.OP_FD_I32X4_SUB),
        i32x4_trunc_sat_f32x4_s("i32x4.trunc_sat_f32x4_s", Prefix.fd, Opcodes.OP_FD_I32X4_TRUNC_SAT_F32X4_S),
        i32x4_trunc_sat_f32x4_u("i32x4.trunc_sat_f32x4_u", Prefix.fd, Opcodes.OP_FD_I32X4_TRUNC_SAT_F32X4_U),
        i32x4_trunc_sat_f64x2_s_zero("i32x4.trunc_sat_f64x2_s_zero", Prefix.fd, Opcodes.OP_FD_I32X4_TRUNC_SAT_F64X2_S_ZERO),
        i32x4_trunc_sat_f64x2_u_zero("i32x4.trunc_sat_f64x2_u_zero", Prefix.fd, Opcodes.OP_FD_I32X4_TRUNC_SAT_F64X2_U_ZERO),
        i64_add("i64.add", Opcodes.OP_I64_ADD),
        i64_and("i64.and", Opcodes.OP_I64_AND),
        i64_clz("i64.clz", Opcodes.OP_I64_CLZ),
        i64_ctz("i64.ctz", Opcodes.OP_I64_CTZ),
        i64_div_s("i64.div_s", Opcodes.OP_I64_DIV_S),
        i64_div_u("i64.div_u", Opcodes.OP_I64_DIV_U),
        i64_eq("i64.eq", Opcodes.OP_I64_EQ),
        i64_eqz("i64.eqz", Opcodes.OP_I64_EQZ),
        i64_extend16_s("i64.extend16_s", Opcodes.OP_I64_EXTEND16_S),
        i64_extend32_s("i64.extend32_s", Opcodes.OP_I64_EXTEND32_S),
        i64_extend8_s("i64.extend8_s", Opcodes.OP_I64_EXTEND8_S),
        i64_extend_i32_s("i64.extend_i32_s", Opcodes.OP_I64_EXTEND_I32_S),
        i64_extend_i32_u("i64.extend_i32_u", Opcodes.OP_I64_EXTEND_I32_U),
        i64_ge_s("i64.ge_s", Opcodes.OP_I64_GE_S),
        i64_ge_u("i64.ge_u", Opcodes.OP_I64_GE_U),
        i64_gt_s("i64.gt_s", Opcodes.OP_I64_GT_S),
        i64_gt_u("i64.gt_u", Opcodes.OP_I64_GT_U),
        i64_le_s("i64.le_s", Opcodes.OP_I64_LE_S),
        i64_le_u("i64.le_u", Opcodes.OP_I64_LE_U),
        i64_lt_s("i64.lt_s", Opcodes.OP_I64_LT_S),
        i64_lt_u("i64.lt_u", Opcodes.OP_I64_LT_U),
        i64_mul("i64.mul", Opcodes.OP_I64_MUL),
        i64_ne("i64.ne", Opcodes.OP_I64_NE),
        i64_or("i64.or", Opcodes.OP_I64_OR),
        i64_popcnt("i64.popcnt", Opcodes.OP_I64_POPCNT),
        i64_reinterpret_f64("i64.reinterpret_f64", Opcodes.OP_I64_REINTERPRET_F64),
        i64_rem_s("i64.rem_s", Opcodes.OP_I64_REM_S),
        i64_rem_u("i64.rem_u", Opcodes.OP_I64_REM_U),
        i64_rotl("i64.rotl", Opcodes.OP_I64_ROTL),
        i64_rotr("i64.rotr", Opcodes.OP_I64_ROTR),
        i64_shl("i64.shl", Opcodes.OP_I64_SHL),
        i64_shr_s("i64.shr_s", Opcodes.OP_I64_SHR_S),
        i64_shr_u("i64.shr_u", Opcodes.OP_I64_SHR_U),
        i64_sub("i64.sub", Opcodes.OP_I64_SUB),
        i64_trunc_f32_s("i64.trunc_f32_s", Opcodes.OP_I64_TRUNC_F32_S),
        i64_trunc_f32_u("i64.trunc_f32_u", Opcodes.OP_I64_TRUNC_F32_U),
        i64_trunc_f64_s("i64.trunc_f64_s", Opcodes.OP_I64_TRUNC_F64_S),
        i64_trunc_f64_u("i64.trunc_f64_u", Opcodes.OP_I64_TRUNC_F64_U),
        i64_trunc_sat_f32_s("i64.trunc_sat_f32_s", Prefix.fc, Opcodes.OP_FC_I64_TRUNC_SAT_F32_S),
        i64_trunc_sat_f32_u("i64.trunc_sat_f32_u", Prefix.fc, Opcodes.OP_FC_I64_TRUNC_SAT_F32_U),
        i64_trunc_sat_f64_s("i64.trunc_sat_f64_s", Prefix.fc, Opcodes.OP_FC_I64_TRUNC_SAT_F64_S),
        i64_trunc_sat_f64_u("i64.trunc_sat_f64_u", Prefix.fc, Opcodes.OP_FC_I64_TRUNC_SAT_F64_U),
        i64_xor("i64.xor", Opcodes.OP_I64_XOR),
        i64x2_abs("i64x2.abs", Prefix.fd, Opcodes.OP_FD_I64X2_ABS),
        i64x2_add("i64x2.add", Prefix.fd, Opcodes.OP_FD_I64X2_ADD),
        i64x2_all_true("i64x2.all_true", Prefix.fd, Opcodes.OP_FD_I64X2_ALL_TRUE),
        i64x2_bitmask("i64x2.bitmask", Prefix.fd, Opcodes.OP_FD_I64X2_BITMASK),
        i64x2_dot_i16x8_s("i64x2.dot_i16x8_s", Prefix.fd, Opcodes.OP_FD_I64X2_DOT_I16X8_S),
        i64x2_extend_high_i32x4_s("i64x2.extend_high_i32x4_s", Prefix.fd, Opcodes.OP_FD_I64X2_EXTEND_HIGH_I32X4_S),
        i64x2_extend_high_i32x4_u("i64x2.extend_high_i32x4_u", Prefix.fd, Opcodes.OP_FD_I64X2_EXTEND_HIGH_I32X4_U),
        i64x2_extend_low_i32x4_s("i64x2.extend_low_i32x4_s", Prefix.fd, Opcodes.OP_FD_I64X2_EXTEND_LOW_I32X4_S),
        i64x2_extend_low_i32x4_u("i64x2.extend_low_i32x4_u", Prefix.fd, Opcodes.OP_FD_I64X2_EXTEND_LOW_I32X4_U),
        i64x2_extmul_high_i32x4_s("i64x2.extmul_high_i32x4_s", Prefix.fd, Opcodes.OP_FD_I64X2_EXTMUL_HIGH_I32X4_S),
        i64x2_extmul_high_i32x4_u("i64x2.extmul_high_i32x4_u", Prefix.fd, Opcodes.OP_FD_I64X2_EXTMUL_HIGH_I32X4_U),
        i64x2_extmul_low_i32x4_s("i64x2.extmul_low_i32x4_s", Prefix.fd, Opcodes.OP_FD_I64X2_EXTMUL_LOW_I32X4_S),
        i64x2_extmul_low_i32x4_u("i64x2.extmul_low_i32x4_u", Prefix.fd, Opcodes.OP_FD_I64X2_EXTMUL_LOW_I32X4_U),
        i64x2_max_s("i64x2.max_s", Prefix.fd, Opcodes.OP_FD_I64X2_MAX_S),
        i64x2_max_u("i64x2.max_u", Prefix.fd, Opcodes.OP_FD_I64X2_MAX_U),
        i64x2_min_s("i64x2.min_s", Prefix.fd, Opcodes.OP_FD_I64X2_MIN_S),
        i64x2_min_u("i64x2.min_u", Prefix.fd, Opcodes.OP_FD_I64X2_MIN_U),
        i64x2_mul("i64x2.mul", Prefix.fd, Opcodes.OP_FD_I64X2_MUL),
        i64x2_neg("i64x2.neg", Prefix.fd, Opcodes.OP_FD_I64X2_NEG),
        i64x2_shl("i64x2.shl", Prefix.fd, Opcodes.OP_FD_I64X2_SHL),
        i64x2_shr_s("i64x2.shr_s", Prefix.fd, Opcodes.OP_FD_I64X2_SHR_S),
        i64x2_shr_u("i64x2.shr_u", Prefix.fd, Opcodes.OP_FD_I64X2_SHR_U),
        i64x2_splat("i64x2.splat", Prefix.fd, Opcodes.OP_FD_I64X2_SPLAT),
        i64x2_sub("i64x2.sub", Prefix.fd, Opcodes.OP_FD_I64X2_SUB),
        i8x16_abs("i8x16.abs", Prefix.fd, Opcodes.OP_FD_I8X16_ABS),
        i8x16_add("i8x16.add", Prefix.fd, Opcodes.OP_FD_I8X16_ADD),
        i8x16_add_sat_s("i8x16.add_sat_s", Prefix.fd, Opcodes.OP_FD_I8X16_ADD_SAT_S),
        i8x16_add_sat_u("i8x16.add_sat_u", Prefix.fd, Opcodes.OP_FD_I8X16_ADD_SAT_U),
        i8x16_all_true("i8x16.all_true", Prefix.fd, Opcodes.OP_FD_I8X16_ALL_TRUE),
        i8x16_avgr_u("i8x16.avgr_u", Prefix.fd, Opcodes.OP_FD_I8X16_AVGR_U),
        i8x16_bitmask("i8x16.bitmask", Prefix.fd, Opcodes.OP_FD_I8X16_BITMASK),
        i8x16_eq("i8x16.eq", Prefix.fd, Opcodes.OP_FD_I8X16_EQ),
        i8x16_ge_s("i8x16.ge_s", Prefix.fd, Opcodes.OP_FD_I8X16_GE_S),
        i8x16_ge_u("i8x16.ge_u", Prefix.fd, Opcodes.OP_FD_I8X16_GE_U),
        i8x16_gt_s("i8x16.gt_s", Prefix.fd, Opcodes.OP_FD_I8X16_GT_S),
        i8x16_gt_u("i8x16.gt_u", Prefix.fd, Opcodes.OP_FD_I8X16_GT_U),
        i8x16_le_s("i8x16.le_s", Prefix.fd, Opcodes.OP_FD_I8X16_LE_S),
        i8x16_le_u("i8x16.le_u", Prefix.fd, Opcodes.OP_FD_I8X16_LE_U),
        i8x16_lt_s("i8x16.lt_s", Prefix.fd, Opcodes.OP_FD_I8X16_LT_S),
        i8x16_lt_u("i8x16.lt_u", Prefix.fd, Opcodes.OP_FD_I8X16_LT_U),
        i8x16_max_s("i8x16.max_s", Prefix.fd, Opcodes.OP_FD_I8X16_MAX_S),
        i8x16_max_u("i8x16.max_u", Prefix.fd, Opcodes.OP_FD_I8X16_MAX_U),
        i8x16_min_s("i8x16.min_s", Prefix.fd, Opcodes.OP_FD_I8X16_MIN_S),
        i8x16_min_u("i8x16.min_u", Prefix.fd, Opcodes.OP_FD_I8X16_MIN_U),
        i8x16_narrow_i16x8_s("i8x16.narrow_i16x8_s", Prefix.fd, Opcodes.OP_FD_I8X16_NARROW_I16X8_S),
        i8x16_narrow_i16x8_u("i8x16.narrow_i16x8_u", Prefix.fd, Opcodes.OP_FD_I8X16_NARROW_I16X8_U),
        i8x16_ne("i8x16.ne", Prefix.fd, Opcodes.OP_FD_I8X16_NE),
        i8x16_neg("i8x16.neg", Prefix.fd, Opcodes.OP_FD_I8X16_NEG),
        i8x16_popcnt("i8x16.popcnt", Prefix.fd, Opcodes.OP_FD_I8X16_POPCNT),
        i8x16_shl("i8x16.shl", Prefix.fd, Opcodes.OP_FD_I8X16_SHL),
        i8x16_shr_s("i8x16.shr_s", Prefix.fd, Opcodes.OP_FD_I8X16_SHR_S),
        i8x16_shr_u("i8x16.shr_u", Prefix.fd, Opcodes.OP_FD_I8X16_SHR_U),
        i8x16_splat("i8x16.splat", Prefix.fd, Opcodes.OP_FD_I8X16_SPLAT),
        i8x16_sub("i8x16.sub", Prefix.fd, Opcodes.OP_FD_I8X16_SUB),
        i8x16_sub_sat_s("i8x16.sub_sat_s", Prefix.fd, Opcodes.OP_FD_I8X16_SUB_SAT_S),
        i8x16_sub_sat_u("i8x16.sub_sat_u", Prefix.fd, Opcodes.OP_FD_I8X16_SUB_SAT_U),
        i8x16_swizzle("i8x16.swizzle", Prefix.fd, Opcodes.OP_FD_I8X16_SWIZZLE),
        nop("nop", Opcodes.OP_NOP),
        ref_is_null("ref.is_null", Opcodes.OP_REF_IS_NULL),
        return_("return", Opcodes.OP_RETURN),
        select("select", Opcodes.OP_SELECT_EMPTY),
        unreachable("unreachable", Opcodes.OP_UNREACHABLE),
        v128_and("v128.and", Prefix.fd, Opcodes.OP_FD_V128_AND),
        v128_andnot("v128.andnot", Prefix.fd, Opcodes.OP_FD_V128_ANDNOT),
        v128_any_true("v128.any_true", Prefix.fd, Opcodes.OP_FD_V128_ANY_TRUE),
        v128_bitselect("v128.bitselect", Prefix.fd, Opcodes.OP_FD_V128_BITSELECT),
        v128_not("v128.not", Prefix.fd, Opcodes.OP_FD_V128_NOT),
        v128_or("v128.or", Prefix.fd, Opcodes.OP_FD_V128_OR),
        v128_xor("v128.xor", Prefix.fd, Opcodes.OP_FD_V128_XOR),
        // atomic
        atomic_fence("atomic.fence", Prefix.fe, Opcodes.OP_FE_ATOMIC_FENCE);
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Simple> optional;

        Simple(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Simple(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Simple(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.SIMPLE;
        }

        @Override
        public Optional<Simple> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) {
            seq.add(this);
        }

        @Override
        public void skip(WasmInputStream is) {
            // no arguments
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Table implements Op {
        // table index
        table_fill("table.fill", Prefix.fc, Opcodes.OP_FC_TABLE_FILL),
        table_get("table.get", Opcodes.OP_TABLE_GET),
        table_grow("table.grow", Prefix.fc, Opcodes.OP_FC_TABLE_GROW),
        table_set("table.set", Opcodes.OP_TABLE_SET),
        table_size("table.size", Prefix.fc, Opcodes.OP_FC_TABLE_SIZE),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Table> optional;

        Table(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Table(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Table(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.TABLE;
        }

        @Override
        public Optional<Table> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveTable(is.u32()));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum TableAndFuncType implements Op {
        // tableidx, typeidx
        call_indirect("call_indirect", Opcodes.OP_CALL_INDIRECT),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<TableAndFuncType> optional;

        TableAndFuncType(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        TableAndFuncType(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        TableAndFuncType(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.TABLE_AND_FUNC_TYPE;
        }

        @Override
        public Optional<TableAndFuncType> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            FuncType type = resolver.resolveFuncType(is.u32());
            int tableIdx = is.u32();
            seq.add(this, resolver.resolveTable(tableIdx), type);
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum TableToTable implements Op {
        // tableidx, tableidx
        table_copy("table.copy", Prefix.fc, Opcodes.OP_FC_TABLE_COPY),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<TableToTable> optional;

        TableToTable(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        TableToTable(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        TableToTable(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.TABLE_AND_TABLE;
        }

        @Override
        public Optional<TableToTable> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveTable(is.u32()), resolver.resolveTable(is.u32()));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }

    }

    enum Tag implements Op {
        // tagidx
        catch_("catch", Opcodes.OP_CATCH),
        throw_("throw", Opcodes.OP_THROW),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Tag> optional;

        Tag(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Tag(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Tag(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.TAG;
        }

        @Override
        public Optional<Tag> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, resolver.resolveTag(is.u32()));
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            is.u32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Instructions which take zero or more types as arguments.
     * For code generation, use the constants in {@link Ops} instead.
     */
    enum Types implements Op {
        select("select", Opcodes.OP_SELECT),
        ;

        private final String name;
        private final Optional<Prefix> prefix;
        private final int opcode;
        private final Optional<Types> optional;

        Types(String name, Optional<Prefix> prefix, int opcode) {
            this.name = name;
            this.prefix = prefix;
            this.opcode = opcode;
            this.optional = Optional.of(this);
        }

        Types(String name, Prefix prefix, int opcode) {
            this(name, prefix.optional(), opcode);
        }

        Types(String name, int opcode) {
            this(name, Optional.empty(), opcode);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public Optional<Prefix> prefix() {
            return prefix;
        }

        @Override
        public Kind kind() {
            return Kind.TYPES;
        }

        @Override
        public Optional<Types> optional() {
            return optional;
        }

        @Override
        public void readFrom(WasmInputStream is, InsnSeq seq, Resolver resolver) throws IOException {
            seq.add(this, is.typeVec());
        }

        @Override
        public void skip(WasmInputStream is) throws IOException {
            int cnt = is.u32();
            for (int i = 0; i < cnt; i ++) {
                is.type();
            }
        }

        @Override
        public String toString() {
            return name;
        }

        public Simple asSimple() {
            return Simple.select;
        }
    }
}



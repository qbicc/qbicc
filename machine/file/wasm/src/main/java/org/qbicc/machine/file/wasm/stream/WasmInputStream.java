package org.qbicc.machine.file.wasm.stream;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.bin.BinaryInput;
import org.qbicc.machine.file.bin.I128;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.Mutability;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.Ops;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.ValType;

/**
 * An input stream for reading raw data from a WASM binary file.
 */
public final class WasmInputStream implements Closeable {
    private final BinaryInput bi;

    public WasmInputStream(BinaryInput bi) {
        Assert.checkNotNullParam("bi", bi);
        this.bi = bi;
    }

    public int u8() throws IOException {
        return bi.uleb32() & 0xff;
    }

    public int u32() throws IOException {
        return bi.uleb32();
    }

    public int s32() throws IOException {
        return bi.sleb32();
    }

    public long u64() throws IOException {
        return bi.uleb64();
    }

    public long s64() throws IOException {
        return bi.sleb64();
    }

    public I128 u128() throws IOException {
        return bi.uleb128();
    }

    public int rawInt() throws IOException {
        return bi.i32le();
    }

    public long rawLong() throws IOException {
        return bi.i64le();
    }

    public float f32() throws IOException {
        return Float.intBitsToFloat(rawInt());
    }

    public double f64() throws IOException {
        return Double.longBitsToDouble(rawLong());
    }

    private static final byte[] NO_BYTES = new byte[0];

    public byte[] byteVec() throws IOException {
        int len = u32();
        if (len == 0) {
            return NO_BYTES;
        }
        return bi.i8array(len);
    }

    private static final int[] NO_INTS = new int[0];

    public int[] u32Vec() throws IOException {
        int len = u32();
        if (len == 0) {
            return NO_INTS;
        }
        int[] vec = new int[len];
        for (int i = 0; i < len; i ++) {
            vec[i] = u32();
        }
        return vec;
    }

    public String utf8() throws IOException {
        return new String(byteVec(), StandardCharsets.UTF_8);
    }

    public int rawByte() throws IOException {
        return bi.u8();
    }

    public Op op() throws IOException {
        Op op = Ops.forOpcode(rawByte()).orElseThrow(WasmInputStream::invalidOpcode);
        if (op instanceof Op.Prefix pfx) {
            op = Ops.forOpcode(pfx, u32()).orElseThrow(WasmInputStream::invalidOpcode);
        }
        return op;
    }

    private static IOException invalidOpcode() {
        return new IOException("Invalid opcode");
    }

    @Override
    public void close() throws IOException {
        bi.close();
    }

    public int optByte() throws IOException {
        return bi.u8opt();
    }

    public int peekRawByte() throws IOException {
        return bi.u8peek();
    }

    public int peekRawByteOpt() throws IOException {
        return bi.u8peekOpt();
    }

    public void skip(final int size) throws IOException {
        bi.skip(size);
    }

    public FuncType funcType() throws IOException {
        if (rawByte() != 0x60) {
            throw new IOException("Expected 0x60 (function type)");
        }
        List<ValType> parameterTypes = typeVec();
        List<ValType> resultTypes = typeVec();
        if (parameterTypes.isEmpty()) {
            if (resultTypes.isEmpty()) {
                return FuncType.EMPTY;
            } else if (resultTypes.size() == 1) {
                return resultTypes.get(0).asFuncTypeReturning();
            }
        }
        return new FuncType(parameterTypes, resultTypes);
    }

    public List<ValType> typeVec() throws IOException {
        int cnt = u32();
        if (cnt == 0) {
            return List.of();
        }
        ValType[] types = new ValType[cnt];
        for (int i = 0; i < cnt; i ++) {
            types[i] = type();
        }
        return List.of(types);
    }

    public ValType type() throws IOException {
        return ValType.forByteValue(rawByte());
    }

    public RefType refType() throws IOException {
        return RefType.forByteValue(rawByte());
    }

    public Mutability mut() throws IOException {
        return switch (rawByte()) {
            case 0x00 -> Mutability.const_;
            case 0x01 -> Mutability.var_;
            default -> throw new IOException("Unexpected mutability type");
        };
    }

    public WasmInputStream fork(final long size) throws IOException {
        return new WasmInputStream(bi.fork(ByteOrder.LITTLE_ENDIAN, size));
    }

    public WasmInputStream fork() throws IOException {
        return new WasmInputStream(bi.fork(ByteOrder.LITTLE_ENDIAN));
    }
}

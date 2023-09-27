package org.qbicc.machine.file.wasm.stream;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.bin.BinaryInput;
import org.qbicc.machine.file.bin.BinaryOutput;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.Mutability;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.ValType;

/**
 * An output stream for write raw data to a WASM binary file.
 */
public final class WasmOutputStream implements Closeable {
    private final BinaryOutput bo;

    public WasmOutputStream(BinaryOutput bo) {
        this.bo = bo;
        bo.order(ByteOrder.LITTLE_ENDIAN);
    }

    public void s33(long val) throws IOException {
        bo.sleb(val);
    }

    public void s128(long lo, long hi) throws IOException {
        bo.sleb(lo, hi);
    }

    public void u128(long lo, long hi) throws IOException {
        bo.uleb(lo, hi);
    }

    public void s64(long val) throws IOException {
        bo.sleb(val);
    }

    public void u64(long val) throws IOException {
        bo.uleb(val);
    }

    public void s32(int val) throws IOException {
        bo.sleb(val);
    }

    public void u32(int val) throws IOException {
        bo.uleb(val);
    }

    public void u32(long val) throws IOException {
        bo.uleb((int)val);
    }

    public void u16(int val) throws IOException {
        bo.uleb(val & 0xffff);
    }

    public void u8(int val) throws IOException {
        bo.uleb(val & 0xff);
    }

    public void rawShort(final int val) throws IOException {
        rawByte(val);
        rawByte(val >> 8);
    }

    public void rawInt(final int val) throws IOException {
        rawShort(val);
        rawShort(val >> 16);
    }

    public void rawLong(final long val) throws IOException {
        rawInt((int) val);
        rawInt((int) (val >> 32L));
    }

    public void f32(final float val) throws IOException {
        rawInt(Float.floatToRawIntBits(val));
    }

    public void f64(final double val) throws IOException {
        rawLong(Double.doubleToRawLongBits(val));
    }

    public void rawByte(int val) throws IOException {
        bo.i8(val);
    }

    public void rawBytes(byte[] val) throws IOException {
        bo.i8array(val);
    }

    public void op(Op op) throws IOException {
        if (op.prefix().isPresent()) {
            op(op.prefix().get());
            u32(op.opcode());
        } else {
            rawByte(op.opcode());
        }
    }

    public void byteVec(byte[] val) throws IOException {
        u32(val.length);
        rawBytes(val);
    }

    public void byteVec(final BinaryInput bi) throws IOException {
        u32(bi.remaining());
        bi.transferTo(bo);
    }

    public void type(final ValType type) throws IOException {
        Assert.checkNotNullParam("type", type);
        bo.i8(type.byteValue());
    }

    public void funcType(final FuncType type) throws IOException {
        Assert.checkNotNullParam("type", type);
        bo.i8(0x60);
        typeVec(type.parameterTypes());
        typeVec(type.resultTypes());
    }

    public void typeVec(final List<ValType> types) throws IOException {
        u32(types.size());
        for (ValType type : types) {
            type(type);
        }
    }

    public void mut(final Mutability mut) throws IOException {
        Assert.checkNotNullParam("mut", mut);
        bo.i8(mut.byteValue());
    }

    public void utf8(final String string) throws IOException {
        byteVec(string.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws IOException {
        bo.close();
    }
}

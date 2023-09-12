package org.qbicc.machine.file.wasm.stream;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Mutability;
import org.qbicc.machine.file.wasm.ValType;

/**
 * An output stream for write raw data to a WASM binary file.
 */
final class WasmOutputStream implements Closeable {
    private final OutputStream os;

    WasmOutputStream(ByteArrayOutputStream os) {
        this.os = os;
    }

    WasmOutputStream(OutputStream os) {
        this.os = os instanceof BufferedOutputStream bos ? bos : os instanceof ByteArrayOutputStream baos ? baos : new BufferedOutputStream(os);
    }

    public void uleb128(int val) throws IOException {
        int b;
        b = val & 0x7f;
        val >>>= 7;
        while (val != 0) {
            b |= 0x80;
            os.write(b);
            b = val & 0x7f;
            val >>>= 7;
        }
        os.write(b);
    }

    public void uleb128(long val) throws IOException {
        int b;
        b = (int) (val & 0x7f);
        val >>>= 7;
        while (val != 0) {
            b |= 0x80;
            os.write(b);
            b = (int) (val & 0x7f);
            val >>>= 7;
        }
        os.write(b);
    }

    public void sleb128(int val) throws IOException {
        int b;
        if (val < 0) {
            b = val & 0x7f;
            val >>= 7;
            while (val != -1) {
                b |= 0x80;
                os.write(b);
                b = val & 0x7f;
                val >>= 7;
            }
            os.write(b);
        } else {
            b = val & 0x7f;
            val >>>= 7;
            // write an extra byte if the sign bit is set on the last one
            while (val != 0 || b >= 0x40) {
                b |= 0x80;
                os.write(b);
                b = val & 0x7f;
                val >>>= 7;
            }
            os.write(b);
        }
    }

    public void sleb128(long val) throws IOException {
        int b;
        if (val < 0) {
            b = (int) (val & 0x7f);
            val >>= 7;
            while (val != -1) {
                b |= 0x80;
                os.write(b);
                b = (int) (val & 0x7f);
                val >>= 7;
            }
            os.write(b);
        } else {
            b = (int) (val & 0x7f);
            val >>>= 7;
            // write an extra byte if the sign bit is set on the last one
            while (val != 0 || b >= 0x40) {
                b |= 0x80;
                os.write(b);
                b = (int) (val & 0x7f);
                val >>>= 7;
            }
            os.write(b);
        }
    }

    public void u128(long lo, long hi) throws IOException {
        int b;
        b = (int) (lo & 0x7f);
        lo = (lo >>> 7) | (hi << 57);
        hi >>>= 7;
        while (lo != 0 && hi != 0) {
            b |= 0x80;
            os.write(b);
            b = (int) (lo & 0x7f);
            lo = (lo >>> 7) | (hi << 57);
            hi >>>= 7;
        }
        os.write(b);
    }

    public void u64(long val) throws IOException {
        uleb128(val);
    }

    public void u32(int val) throws IOException {
        uleb128(val);
    }

    public void u32(long val) throws IOException {
        uleb128((int)val);
    }

    public void u16(int val) throws IOException {
        uleb128(val & 0xffff);
    }

    public void u8(int val) throws IOException {
        uleb128(val & 0xff);
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
        os.write(val);
    }

    public void rawBytes(byte[] val) throws IOException {
        os.write(val);
    }

    public void byteVec(byte[] val) throws IOException {
        u32(val.length);
        rawBytes(val);
    }

    public void type(final ValType type) throws IOException {
        Assert.checkNotNullParam("type", type);
        os.write(type.byteValue());
    }

    public void mut(final Mutability mut) throws IOException {
        Assert.checkNotNullParam("mut", mut);
        os.write(mut.byteValue());
    }

    public void utf8(final String string) throws IOException {
        byteVec(string.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws IOException {
        os.close();
    }
}

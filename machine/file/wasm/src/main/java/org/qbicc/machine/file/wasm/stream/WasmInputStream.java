package org.qbicc.machine.file.wasm.stream;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.Mutability;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.ValType;

/**
 * An input stream for reading raw data from a WASM binary file.
 */
final class WasmInputStream implements Closeable {
    private final InputStream is;
    private long offset;

    WasmInputStream(BufferedInputStream is) {
        this((InputStream) is);
    }

    WasmInputStream(ByteArrayInputStream is) {
        this((InputStream) is);
    }

    WasmInputStream(InputStream is) {
        this.is = is instanceof ByteArrayInputStream bais ? bais : is instanceof BufferedInputStream bis ? bis : new BufferedInputStream(is);
        assert this.is.markSupported();
    }

    public long offset() {
        return offset;
    }

    public int uleb128_32() throws IOException {
        int res;
        int val = 0;
        int shift = 0;
        do {
            res = rawByte();
            val |= shl((res & 0x7f), shift);
            shift += 7;
        } while (res >= 0x80);
        return val;
    }

    public int sleb128_32() throws IOException {
        int res;
        int val = 0;
        int shift = 0;
        do {
            res = rawByte();
            val |= shl((res & 0x7f), shift);
            shift += 7;
        } while (res >= 0x80);
        if (shift < 32 && (res & 0x40) != 0) {
            val |= shl(~0, shift);
        }
        return val;
    }

    public long uleb128_64() throws IOException {
        int res;
        long val = 0;
        int shift = 0;
        do {
            res = rawByte();
            val |= shl(res & 0x7fL, shift);
            shift += 7;
        } while (res >= 0x80);
        return val;
    }

    public long sleb128_64() throws IOException {
        int res;
        long val = 0;
        int shift = 0;
        do {
            res = rawByte();
            val |= shl(res & 0x7fL, shift);
            shift += 7;
        } while (res >= 0x80);
        if (shift < 64 && (res & 0x40) != 0) {
            val |= shl(~0L, shift);
        }
        return val;
    }

    public I128 u128() throws IOException {
        int res;
        long low = 0;
        long high = 0;
        int shift = 0;
        do {
            res = optByte();
            if (res == -1) {
                throw new EOFException();
            }
            high |= shl(res & 0x7fL, shift - 64);
            low |= shl(res & 0x7fL, shift);
            shift += 7;
        } while (res >= 0x80);
        return new I128(low, high);
    }

    private static int shl(int num, int cnt) {
        return cnt < 0 ? lshr(num, -cnt) : cnt > 32 ? 0 : num << cnt;
    }

    private static int lshr(int num, int cnt) {
        return cnt < 0 ? shl(num, -cnt) : cnt > 32 ? 0 : num >>> cnt;
    }

    private static long shl(long num, int cnt) {
        return cnt < 0 ? lshr(num, -cnt) : cnt > 64 ? 0 : num << cnt;
    }

    private static long lshr(long num, int cnt) {
        return cnt < 0 ? shl(num, -cnt) : cnt > 64 ? 0 : num >>> cnt;
    }

    public int u8() throws IOException {
        return uleb128_32() & 0xff;
    }

    public int u16() throws IOException {
        return uleb128_32() & 0xffff;
    }

    public int u32() throws IOException {
        return uleb128_32();
    }

    public long u64() throws IOException {
        return uleb128_64();
    }

    public long s64() throws IOException {
        return sleb128_64();
    }

    public int rawShort() throws IOException {
        return rawByte() | rawByte() << 8;
    }

    public int rawInt() throws IOException {
        return rawShort() | rawShort() << 16;
    }

    public float f32() throws IOException {
        return Float.intBitsToFloat(rawInt());
    }

    public long rawLong() throws IOException {
        return rawInt() & 0xFFFF_FFFFL | (long) rawInt() << 32L;
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
        byte[] bytes = new byte[len];
        int res = is.read(bytes);
        if (res == -1) {
            throw new EOFException();
        }
        if (res < bytes.length) {
            // try again
            int pos = res;
            do {
                res = is.read(bytes, pos, bytes.length - pos);
                if (res == -1) {
                    throw new EOFException();
                }
                pos += res;
                offset += res;
            } while (pos < bytes.length);
        }
        return bytes;
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
        int res = optByte();
        if (res == -1) {
            throw new EOFException();
        }
        return res;
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    public int optByte() throws IOException {
        int res = is.read();
        if (res >= 0) {
            offset++;
        }
        return res;
    }

    public int peekRawByte() throws IOException {
        is.mark(1);
        int res = is.read();
        is.reset();
        if (res == -1) {
            throw new EOFException();
        }
        // do not nudge offset
        return res;
    }

    public void skip(final int size) throws IOException {
        long cnt = 0;
        while (cnt < size) {
            long skipped = is.skip(size - cnt);
            if (skipped == 0) {
                // provoke a EOF exception if possible
                rawByte();
                // no EOF; advance cnt (but not offset, because rawByte already did)
                cnt ++;
            }
            cnt += skipped;
            offset += skipped;
        }
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
}

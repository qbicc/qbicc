package org.qbicc.machine.file.bin;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A binary input stream that is more powerful than a regular input stream.
 */
public abstract class BinaryInput implements Closeable {

    public static BinaryInput create(ByteBuffer buffer) {
        return new BufferBinaryInput(buffer.duplicate().order(buffer.order()));
    }

    public static BinaryInput create(byte[] bytes, int off, int len, ByteOrder order) {
        return new BufferBinaryInput(ByteBuffer.wrap(bytes, off, len).order(order));
    }

    public static BinaryInput create(byte[] bytes, ByteOrder order) {
        return create(bytes, 0, bytes.length, order);
    }

    public static BinaryInput open(Path path, ByteOrder byteOrder) throws IOException {
        return open(FileChannel.open(path, StandardOpenOption.READ), byteOrder);
    }

    static BufferBinaryInput open(final FileChannel ch, final ByteOrder byteOrder) throws IOException {
        long size = ch.size();
        try {
            try {
                MappedByteBuffer mapped = ch.map(FileChannel.MapMode.READ_ONLY, 0, size);
                mapped.order(byteOrder);
                return new MappedBufferBinaryInput(ch, mapped, new AtomicInteger(1));
            } catch (UnsupportedOperationException ignored) {
                // can't map, have to read it :(
                if (size > Integer.MAX_VALUE) {
                    throw new IOException("File is too large");
                }
                ByteBuffer buffer = ByteBuffer.allocateDirect((int) size);
                int res;
                do {
                    res = ch.read(buffer);
                } while (buffer.position() < size && res != -1);
                ch.close();
                buffer.flip();
                return new BufferBinaryInput(buffer);
            }
        } catch (Throwable t) {
            try {
                ch.close();
            } catch (Throwable t2) {
                t.addSuppressed(t2);
            }
            throw t;
        }
    }

    public abstract ByteOrder order();

    /**
     * Change the byte order.
     *
     * @param newOrder the new byte order (must not be {@code null})
     * @return the previous byte order (not {@code null})
     */
    public abstract ByteOrder order(ByteOrder newOrder);

    /**
     * Get the relative position.
     *
     * @return the relative position in bytes
     */
    public abstract long position();

    /**
     * Get the number of remaining bytes available to read.
     *
     * @return the remaining byte count
     */
    public abstract long remaining();

    /**
     * Fork this input starting at the current position.
     * The fork will have an independent read position starting at 0.
     * Closing the fork does not close this stream (or vice-versa), but the fork <em>must</em> be closed.
     *
     * @return the fork (not {@code null})
     */
    public BinaryInput fork() throws IOException {
        return fork(order());
    }

    /**
     * Fork this input starting at the current position.
     * The fork will have an independent read position starting at 0.
     *
     * @param order the byte order of the fork (must not be {@code null})
     * @return the fork (not {@code null})
     */
    public abstract BinaryInput fork(ByteOrder order) throws IOException;

    /**
     * Fork this input starting at the current position with a limited size.
     * The fork will have an independent read position starting at 0.
     *
     * @param size the maximum size of the fork
     * @return the fork (not {@code null})
     */
    public BinaryInput fork(long size) throws IOException {
        return fork(order(), size);
    }

    /**
     * Fork this input starting at the current position with a limited size.
     * The fork will have an independent read position starting at 0.
     *
     * @param order the byte order of the fork (must not be {@code null})
     * @param size the maximum size of the fork
     * @return the fork (not {@code null})
     */
    public abstract BinaryInput fork(final ByteOrder order, final long size) throws IOException;

    /**
     * Skip some data.
     *
     * @param amount the number of bytes to skip
     * @throws EOFException if the end of file is reached
     * @throws IOException if an error occurs
     */
    public abstract void skip(long amount) throws IOException;

    /**
     * Read an optional byte.
     *
     * @return the unsigned byte value, or -1 if the end of file is reached
     * @throws IOException if an error occurs
     */
    public abstract int u8opt() throws IOException;

    /**
     * Peek at the next byte without consuming it.
     *
     * @return the unsigned byte value, or -1 if the end of file would be reached
     * @throws IOException if an error occurs
     */
    public abstract int u8peekOpt() throws IOException;

    public int u8() throws IOException {
        int res = u8opt();
        if (res == -1) {
            throw new EOFException();
        }
        return res;
    }

    public int u8peek() throws IOException {
        int res = u8peekOpt();
        if (res == -1) {
            throw new EOFException();
        }
        return res;
    }

    public int s8() throws IOException {
        return (byte) u8();
    }

    public int u16le() throws IOException {
        return u8() | u8() << 8;
    }

    public int u16be() throws IOException {
        return u8() << 8 | u8();
    }

    public int u16() throws IOException {
        return order() == ByteOrder.LITTLE_ENDIAN ? u16le() : u16be();
    }

    public int s16le() throws IOException {
        return (short) u16le();
    }

    public int s16be() throws IOException {
        return (short) u16be();
    }

    public int s16() throws IOException {
        return (short) u16();
    }

    public long u32le() throws IOException {
        return Integer.toUnsignedLong(i32le());
    }

    public long u32be() throws IOException {
        return Integer.toUnsignedLong(i32be());
    }

    public long u32() throws IOException {
        return order() == ByteOrder.LITTLE_ENDIAN ? u32le() : u32be();
    }

    public int i32le() throws IOException {
        return u16le() | u16le() << 16;
    }

    public int i32be() throws IOException {
        return u16be() << 16 | u16be();
    }

    public int i32() throws IOException {
        return order() == ByteOrder.LITTLE_ENDIAN ? i32le() : i32be();
    }

    public long i64le() throws IOException {
        return u32le() | u32le() << 32;
    }

    public long i64be() throws IOException {
        return u32be() << 32 | u32be();
    }

    public long i64() throws IOException {
        return order() == ByteOrder.LITTLE_ENDIAN ? i64le() : i64be();
    }

    public float f32le() throws IOException {
        return Float.intBitsToFloat(i32le());
    }

    public float f32be() throws IOException {
        return Float.intBitsToFloat(i32be());
    }

    public float f32() throws IOException {
        return Float.intBitsToFloat(i32());
    }

    public double f64le() throws IOException {
        return Double.longBitsToDouble(i64le());
    }

    public double f64be() throws IOException {
        return Double.longBitsToDouble(i64be());
    }

    public double f64() throws IOException {
        return Double.longBitsToDouble(i64());
    }

    public long[] i64array(int count) throws IOException {
        long[] array = new long[count];
        i64array(array);
        return array;
    }

    public void i64array(long[] array, int off, int len) throws IOException {
        for (int i = 0; i < len; i ++) {
            array[off + i] = i64();
        }
    }

    public void i64array(long[] array) throws IOException {
        i64array(array, 0, array.length);
    }

    public int[] i32array(int count) throws IOException {
        int[] array = new int[count];
        i32array(array);
        return array;
    }

    public void i32array(int[] array, int off, int len) throws IOException {
        for (int i = 0; i < len; i ++) {
            array[off + i] = i32();
        }
    }

    public void i32array(int[] array) throws IOException {
        i32array(array, 0, array.length);
    }

    public short[] i16array(int count) throws IOException {
        short[] array = new short[count];
        i16array(array);
        return array;
    }

    public void i16array(short[] array, int off, int len) throws IOException {
        for (int i = 0; i < len; i ++) {
            array[off + i] = (short) u16();
        }
    }

    public void i16array(short[] array) throws IOException {
        i16array(array, 0, array.length);
    }

    public byte[] i8array(int count) throws IOException {
        byte[] array = new byte[count];
        i8array(array);
        return array;
    }

    public void i8array(byte[] array, int off, int len) throws IOException {
        for (int i = 0; i < len; i ++) {
            array[off + i] = (byte) u8();
        }
    }

    public void i8array(byte[] array) throws IOException {
        i8array(array, 0, array.length);
    }

    public static final int UTF8_ENCODING_MASK = 0b111 << 29;
    public static final int UTF8_CODE_POINT_MASK = ~UTF8_ENCODING_MASK;
    public static final int UTF8_ENCODING_1 = 0 << 29;
    public static final int UTF8_ENCODING_2 = 1 << 29;
    public static final int UTF8_ENCODING_3 = 2 << 29;
    public static final int UTF8_ENCODING_4 = 3 << 29;

    public int utf8codePoint(boolean replaceErrors) throws IOException {
        int a = u8();
        if (a < 0b1_0000000) {
            // ascii
            return a | UTF8_ENCODING_1;
        }
        if (a < 0b110_00000 || a > 0b11110_111) {
            return invalid(replaceErrors);
        }
        // a is valid
        int b = u8peek();
        if ((b & 0b11_000000) != 0b10_000000) {
            return invalid(replaceErrors);
        }
        // consume
        u8();
        if (a < 0b1110_0000) {
            // two-byte form
            return (a & 0b000_11111) << 6 | b & 0b00_111111 | UTF8_ENCODING_2;
        }
        int c = u8peek();
        if ((c & 0b11_000000) != 0b10_000000) {
            return invalid(replaceErrors);
        }
        // consume
        u8();
        if (a < 0b1111_0000) {
            // three-byte form
            return (a & 0b000_11111) << 12 | (b & 0b00_111111) << 6 | c & 0b00_111111 | UTF8_ENCODING_3;
        }
        int d = u8peek();
        if ((d & 0b11_000000) != 0b10_000000) {
            return invalid(replaceErrors);
        }
        // consume
        u8();
        // four-byte form
        return (a & 0b000_11111) << 18 | (b & 0b00_111111) << 12 | (c & 0b00_111111) << 6 | d & 0b00_111111 | UTF8_ENCODING_4;
    }

    public StringBuilder utf8z(StringBuilder sb, boolean replaceErrors) throws IOException {
        int cp;
        while ((cp = utf8codePoint(replaceErrors)) != (0 | UTF8_ENCODING_1)) {
            sb.appendCodePoint(cp & UTF8_CODE_POINT_MASK);
        }
        return sb;
    }

    private int invalid(boolean replaceErrors) throws IOException {
        if (replaceErrors) {
            return '�';
        } else {
            throw new IOException("Invalid UTF-8 data");
        }
    }

    public int uleb32() throws IOException {
        int res;
        int val = 0;
        int shift = 0;
        do {
            res = u8();
            val |= shl((res & 0x7f), shift);
            shift += 7;
        } while (res >= 0x80);
        return val;
    }

    public long uleb64() throws IOException {
        int res;
        long val = 0;
        int shift = 0;
        do {
            res = u8();
            val |= shl(res & 0x7fL, shift);
            shift += 7;
        } while (res >= 0x80);
        return val;
    }

    public I128 uleb128() throws IOException {
        int res;
        long low = 0;
        long high = 0;
        int shift = 0;
        do {
            res = u8();
            high |= shl(res & 0x7fL, shift - 64);
            low |= shl(res & 0x7fL, shift);
            shift += 7;
        } while (res >= 0x80);
        return new I128(low, high);
    }

    public int sleb32() throws IOException {
        int res;
        int val = 0;
        int shift = 0;
        do {
            res = u8();
            val |= shl((res & 0x7f), shift);
            shift += 7;
        } while (res >= 0x80);
        if (shift < 32 && (res & 0x40) != 0) {
            val |= shl(~0, shift);
        }
        return val;
    }

    public long sleb64() throws IOException {
        int res;
        long val = 0;
        int shift = 0;
        do {
            res = u8();
            val |= shl(res & 0x7fL, shift);
            shift += 7;
        } while (res >= 0x80);
        if (shift < 64 && (res & 0x40) != 0) {
            val |= shl(~0L, shift);
        }
        return val;
    }

    public I128 sleb128() throws IOException {
        int res;
        long low = 0;
        long high = 0;
        int shift = 0;
        do {
            res = u8();
            high |= shl(res & 0x7fL, shift - 64);
            low |= shl(res & 0x7fL, shift);
            shift += 7;
        } while (res >= 0x80);
        if ((res & 0x40) != 0) {
            if (shift < 64) {
                high = ~ 0L;
                low |= shl(~ 0L, shift);
            } else if (shift < 128) {
                high |= shl(~ 0L, shift - 64);
            }
        }
        return new I128(low, high);
    }

    private static int shl(int num, int cnt) {
        return cnt < 0 ? lshr(num, -cnt) : cnt >= 32 ? 0 : num << cnt;
    }

    private static int lshr(int num, int cnt) {
        return cnt < 0 ? shl(num, -cnt) : cnt >= 32 ? 0 : num >>> cnt;
    }

    private static long shl(long num, int cnt) {
        return cnt < 0 ? lshr(num, -cnt) : cnt >= 64 ? 0 : num << cnt;
    }

    private static long lshr(long num, int cnt) {
        return cnt < 0 ? shl(num, -cnt) : cnt >= 64 ? 0 : num >>> cnt;
    }

    public abstract void transferTo(BinaryOutput bo) throws IOException;

    public abstract void transferTo(FileChannel fc) throws IOException;

    public void transferTo(OutputStream os) throws IOException {
        if (os instanceof FileOutputStream fos) {
            transferTo(fos.getChannel());
            return;
        }
        OutputStream wos = os instanceof ByteArrayOutputStream baos ? baos : os instanceof BufferedOutputStream bos ? bos : new BufferedOutputStream(os);
        int i;
        while ((i = u8opt()) != -1) {
            wos.write(i);
        }
        wos.flush();
    }

    public void transferTo(Path path) throws IOException {
        try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            transferTo(os);
        }
    }

    public abstract void close() throws IOException;
}

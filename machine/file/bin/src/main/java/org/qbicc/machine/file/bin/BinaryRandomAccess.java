package org.qbicc.machine.file.bin;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A binary view which allows for random-access (non-streaming) reading and writing.
 */
public abstract class BinaryRandomAccess implements Closeable {
    BinaryRandomAccess() {
    }

    public abstract ByteOrder order();

    public static BinaryRandomAccess create(ByteBuffer buffer) {
        return new BufferBinaryRandomAccess(buffer);
    }

    /**
     * Change the byte order.
     *
     * @param newOrder the new byte order (must not be {@code null})
     * @return the previous byte order (not {@code null})
     */
    public abstract ByteOrder order(ByteOrder newOrder);

    public abstract BinaryInput reread(long offset, long size);

    public abstract BinaryOutput rewrite(long offset, long size);

    // 8

    public abstract int i8(long offset);

    public abstract void i8(long offset, int value);

    public byte[] i8ArrayGet(long offset, int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i ++) {
            bytes[i] = (byte) i8(offset + i);
        }
        return bytes;
    }

    public void i8ArrayPut(long offset, byte[] bytes, int off, int len) {
        for (int i = 0; i < len; i ++) {
            i8(offset + i, bytes[off + i]);
        }
    }

    // 16

    public int i16(long offset) {
        return order() == ByteOrder.LITTLE_ENDIAN ? i16le(offset) : i16be(offset);
    }

    public int i16le(long offset) {
        return i8(offset) | i8(offset + 1) << 8;
    }

    public int i16be(long offset) {
        return i8(offset) << 8 | i8(offset + 1);
    }

    public void i16(long offset, int value) {
        if (order() == ByteOrder.LITTLE_ENDIAN) {
            i16le(offset, value);
        } else {
            i16be(offset, value);
        }
    }

    public void i16le(long offset, int value) {
        i8(offset, value);
        i8(offset + 1, value >> 8);
    }

    public void i16be(long offset, int value) {
        i8(offset, value >> 8);
        i8(offset + 1, value);
    }

    // 32

    public int i32(long offset) {
        return order() == ByteOrder.LITTLE_ENDIAN ? i32le(offset) : i32be(offset);
    }

    public int i32le(long offset) {
        return i16le(offset) | i16le(offset + 2) << 16;
    }

    public int i32be(long offset) {
        return i16be(offset) << 16 | i16be(offset + 2);
    }

    public void i32(long offset, int value) {
        if (order() == ByteOrder.LITTLE_ENDIAN) {
            i32le(offset, value);
        } else {
            i32be(offset, value);
        }
    }

    public void i32le(long offset, int value) {
        i16le(offset, value);
        i16le(offset + 2, value >> 16);
    }

    public void i32be(long offset, int value) {
        i16be(offset, value >> 16);
        i16be(offset + 2, value);
    }

    // 64

    public long i64(long offset) {
        return order() == ByteOrder.LITTLE_ENDIAN ? i64le(offset) : i64be(offset);
    }

    public long i64le(long offset) {
        return (long)i32le(offset) | (long)i32le(offset + 4) << 32L;
    }

    public long i64be(long offset) {
        return (long)i32be(offset) << 32L | (long)i32be(offset + 4);
    }

    public void i64(long offset, long value) {
        if (order() == ByteOrder.LITTLE_ENDIAN) {
            i64le(offset, value);
        } else {
            i64be(offset, value);
        }
    }

    public void i64le(long offset, long value) {
        i32le(offset, (int) value);
        i32le(offset + 4, (int) (value >> 32));
    }

    public void i64be(long offset, long value) {
        i32be(offset, (int) (value >> 32));
        i32be(offset + 4, (int) value);
    }
}

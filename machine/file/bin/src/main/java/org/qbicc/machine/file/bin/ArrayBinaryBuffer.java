package org.qbicc.machine.file.bin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;

/**
 *
 */
public final class ArrayBinaryBuffer implements BinaryBuffer {
    // 16kB per buffer
    private static final int OUTER_BITS = 13;
    private static final int INNER_SIZE = 1 << OUTER_BITS;
    private static final int INNER_MASK = INNER_SIZE - 1;

    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private long size;
    private byte[][] arrays;

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public ByteOrder setByteOrder(final ByteOrder newOrder) {
        final ByteOrder oldOrder = this.byteOrder;
        byteOrder = newOrder;
        return oldOrder;
    }

    private byte[] getOrCreate(long position) {
        int outerIdx = Math.toIntExact(position >> OUTER_BITS);
        byte[][] arrays = this.arrays;
        if (arrays == null) {
            this.arrays = arrays = new byte[Math.max(8, outerIdx)][];
        }
        return arrays[outerIdx];
    }

    public long size() {
        return size;
    }

    public long growBy(final long amount) {
        getOrCreate(size + amount - 1);
        return size;
    }

    public void growTo(final long newSize) {
        getOrCreate(newSize - 1);
    }

    public int getInt(final long offset) {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            return getShortUnsigned(offset) | getShortUnsigned(offset + 2) << 16;
        } else {
            return getShortUnsigned(offset) << 16 | getShortUnsigned(offset + 2);
        }
    }

    public void putInt(final long offset, final int value) {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            putShort(offset, (short) value);
            putShort(offset + 2, value >>> 16);
        } else {
            putShort(offset, value >>> 16);
            putShort(offset + 2, (short) value);
        }
    }

    public short getShort(final long offset) {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            return (short) (getByteUnsigned(offset) | getByteUnsigned(offset + 1) << 8);
        } else {
            return (short) (getByteUnsigned(offset) << 8 | getByteUnsigned(offset + 1));
        }
    }

    public void putShort(final long offset, final short value) {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            putByte(offset, (byte) value);
            putByte(offset + 1, value >>> 8);
        } else {
            putByte(offset, value >>> 8);
            putByte(offset + 1, (byte) value);
        }
    }

    public byte getByte(final long offset) {
        return getOrCreate(offset)[(int) (offset & INNER_MASK)];
    }

    public void putByte(final long offset, final byte value) {
        getOrCreate(offset)[(int) (offset & INNER_MASK)] = value;
        if (offset >= size) size = offset + 1;
    }

    public long getLong(final long offset) {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            return getIntUnsigned(offset) | getIntUnsigned(offset + 4) << 32;
        } else {
            return getIntUnsigned(offset) << 32 | getIntUnsigned(offset + 2);
        }
    }

    public void putLong(final long offset, final long value) {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            putInt(offset, (int) value);
            putInt(offset + 4, value >>> 32);
        } else {
            putInt(offset, value >>> 32);
            putInt(offset + 4, (int) value);
        }
    }

    public void getBytes(long offset, final byte[] b, int off, int len) {
        byte[] src;
        int cnt;
        int srcOffs;
        while (len > 0) {
            src = getOrCreate(offset);
            srcOffs = len & INNER_MASK;
            cnt = Math.min(len, INNER_SIZE - srcOffs);
            System.arraycopy(src, srcOffs, b, off, cnt);
            off += cnt;
            len -= cnt;
            offset += cnt;
        }
    }

    public void putBytes(long offset, final byte[] b, int off, int len) {
        byte[] dst;
        int cnt;
        int dstOffs;
        while (len > 0) {
            dst = getOrCreate(offset);
            dstOffs = len & INNER_MASK;
            cnt = Math.min(len, INNER_SIZE - dstOffs);
            System.arraycopy(b, off, dst, dstOffs, cnt);
            off += cnt;
            len -= cnt;
            offset += cnt;
        }
    }

    public void putBytes(long position, final BinaryBuffer buf, long offset, long size) {
        byte[] dst;
        int cnt;
        while (size > 0) {
            dst = getOrCreate(offset);
            cnt = (int) Math.min(size, INNER_SIZE - offset);
            buf.getBytes(offset, dst, 0, cnt);
            size -= cnt;
            position += cnt;
            offset += cnt;
        }
    }

    public void writeTo(long offset, final ByteBuffer byteBuffer) {
        int rem;
        while ((rem = byteBuffer.remaining()) > 0) {
            final byte[] src = getOrCreate(offset);
            final int io = (int) (offset & INNER_MASK);
            byteBuffer.put(src, io, Math.min(rem, INNER_SIZE - io));
        }
    }

    public void writeTo(final GatheringByteChannel channel) throws IOException {
        writeTo(channel, 0, size());
    }

    public void writeTo(final GatheringByteChannel fc, long offset, long cnt) throws IOException {
        while (cnt > 0) {
            final byte[] b = getOrCreate(offset);
            final int wcnt = (int) (offset & INNER_MASK);
            fc.write(ByteBuffer.wrap(b, 0, wcnt));
            offset += wcnt;
            cnt -= wcnt;
        }
    }

    public void close() {
    }
}

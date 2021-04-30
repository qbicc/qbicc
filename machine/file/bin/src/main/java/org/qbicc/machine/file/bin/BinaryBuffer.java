package org.qbicc.machine.file.bin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 *
 */
public interface BinaryBuffer extends AutoCloseable {
    static BinaryBuffer openRead(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            final long s = channel.size();
            if (s > 0x7FFF_FFFF) {
                throw new IllegalArgumentException("Large files not yet supported");
            }
            return overBuffer(channel.map(FileChannel.MapMode.READ_ONLY, 0, s));
        }
    }

    static BinaryBuffer overBuffer(ByteBuffer buf) {
        if (buf instanceof MappedByteBuffer) {
            return overBuffer((MappedByteBuffer) buf);
        } else {
            return new BufferBinaryBuffer(buf);
        }
    }

    static BinaryBuffer overBuffer(MappedByteBuffer buf) {
        return new MappedBinaryBuffer(buf);
    }

    ByteOrder getByteOrder();

    ByteOrder setByteOrder(ByteOrder newOrder);

    long size();

    long growBy(long amount);

    void growTo(long newSize);

    int getInt(long offset);

    default long getIntUnsigned(long offset) {
        return getInt(offset) & 0xFFFF_FFFFL;
    }

    void putInt(long offset, int value);

    default void putInt(long offset, long value) {
        Utils.rangeCheck32(value);
        putInt(offset, (int) value);
    }

    short getShort(long offset);

    default int getShortUnsigned(long offset) {
        return getShort(offset) & 0xFFFF;
    }

    void putShort(long offset, short value);

    default void putShort(long offset, int value) {
        Utils.rangeCheck16(value);
        putShort(offset, (short) value);
    }

    default void putShort(long offset, long value) {
        Utils.rangeCheck16(value);
        putShort(offset, (short) value);
    }

    byte getByte(long offset);

    default short getByteUnsigned(long offset) {
        return (short) (getByte(offset) & 0xFF);
    }

    void putByte(long offset, byte value);

    default void putByte(long offset, int value) {
        Utils.rangeCheck8(value);
        putByte(offset, (byte) value);
    }

    default void putByte(long offset, long value) {
        Utils.rangeCheck8(value);
        putByte(offset, (byte) value);
    }

    long getLong(long offset);

    void putLong(long offset, long value);

    void getBytes(long offset, byte[] b, int off, int len);

    default void getBytes(long offset, byte[] b) {
        getBytes(offset, b, 0, b.length);
    }

    void putBytes(long offset, byte[] b, int off, int len);

    default void putBytes(long offset, byte[] b) {
        putBytes(offset, b, 0, b.length);
    }

    void putBytes(long position, BinaryBuffer buf, long offset, long size);

    default WritingIterator writingIterator(long position) {
        return new WritingIterator(this, position, false);
    }

    default WritingIterator appendingIterator() {
        return new WritingIterator(this, size(), true);
    }

    default ReadingIterator readingIterator(long position) {
        return new ReadingIterator(this, position);
    }

    void writeTo(long offset, ByteBuffer byteBuffer);

    void writeTo(GatheringByteChannel channel) throws IOException;

    void close();

    void writeTo(GatheringByteChannel fc, long offset, long cnt) throws IOException;

    ByteBuffer getBuffer(long offset, long size);

    abstract class BufferIterator implements AutoCloseable {
        final BinaryBuffer backingBuffer;
        long position;

        BufferIterator(BinaryBuffer backingBuffer, long position) {
            this.backingBuffer = backingBuffer;
            this.position = position;
        }

        public BinaryBuffer getBackingBuffer() {
            return backingBuffer;
        }

        public long position() {
            return position;
        }

        public abstract void close();

        public void skip(long count) {
            position += count;
        }

        public void alignTo(final int alignment) {
            if (Integer.bitCount(alignment) != 1) {
                throw new IllegalArgumentException("Invalid alignment");
            }
            final int mask = alignment - 1;
            int offs = (int) (position & mask);
            if (offs > 0) {
                skip(mask - offs + 1);
            }
        }
    }

    final class WritingIterator extends BufferIterator {
        private final boolean appending;

        WritingIterator(final BinaryBuffer backingBuffer, final long position, final boolean appending) {
            super(backingBuffer, position);
            this.appending = appending;
        }

        public boolean isAppending() {
            return appending;
        }

        public void putByte(long value) {
            final long position = this.position;
            backingBuffer.putByte(position, value);
            this.position = position + 1;
        }

        public void putByte(int value) {
            final long position = this.position;
            backingBuffer.putByte(position, value);
            this.position = position + 1;
        }

        public void putByte(byte value) {
            final long position = this.position;
            backingBuffer.putByte(position, value);
            this.position = position + 1;
        }

        public void putShort(long value) {
            final long position = this.position;
            backingBuffer.putShort(position, value);
            this.position = position + 2;
        }

        public void putShort(int value) {
            final long position = this.position;
            backingBuffer.putShort(position, value);
            this.position = position + 2;
        }

        public void putShort(short value) {
            final long position = this.position;
            backingBuffer.putShort(position, value);
            this.position = position + 2;
        }

        public void putInt(int value) {
            final long position = this.position;
            backingBuffer.putInt(position, value);
            this.position = position + 4;
        }

        public void putInt(long value) {
            final long position = this.position;
            backingBuffer.putInt(position, value);
            this.position = position + 4;
        }

        public void putLong(long value) {
            final long position = this.position;
            backingBuffer.putLong(position, value);
            this.position = position + 8;
        }

        public void putBytes(byte[] b, int off, int len) {
            final long position = this.position;
            backingBuffer.putBytes(position, b, off, len);
            this.position = position + len;
        }

        public void putBytes(byte[] b) {
            putBytes(b, 0, b.length);
        }

        public void putBytes(BinaryBuffer buf, long pos, long size) {
            final long position = this.position;
            backingBuffer.putBytes(position, buf, pos, size);
            this.position = position + size;
        }

        public void putCodePoint(int codePoint, boolean encodeZero) {
            final long position = this.position;
            final BinaryBuffer bb = backingBuffer;
            if (codePoint == 0 && !encodeZero || 0 < codePoint && codePoint < 0x80) {
                bb.putByte(position, codePoint);
                this.position = position + 1;
            } else if (codePoint < 0x800) {
                // do it in reverse order to detect end-of-buffer
                bb.putByte(position + 1, codePoint & 0b0011_1111 | 0b1000_0000);
                bb.putByte(position, codePoint >>> 6 | 0b1100_0000);
                this.position = position + 2;
            } else if (codePoint < 0x10000) {
                bb.putByte(position + 2, codePoint & 0b0011_1111 | 0b1000_0000);
                bb.putByte(position + 1, codePoint >>> 6 & 0b0011_1111 | 0b1000_0000);
                bb.putByte(position, codePoint >>> 12 | 0b1110_0000);
                this.position = position + 3;
            } else if (codePoint < 0x110000) {
                bb.putByte(position + 3, codePoint & 0b0011_1111 | 0b1000_0000);
                bb.putByte(position + 2, codePoint >>> 6 & 0b0011_1111 | 0b1000_0000);
                bb.putByte(position + 1, codePoint >>> 12 & 0b0011_1111 | 0b1000_0000);
                bb.putByte(position, codePoint >>> 18 | 0b1111_0000);
                this.position = position + 4;
            } else {
                putCodePoint('�', false);
            }
        }

        public void close() {
        }
    }

    final class ReadingIterator extends BufferIterator {
        ReadingIterator(final BinaryBuffer backingBuffer, final long position) {
            super(backingBuffer, position);
        }

        public byte getByte() {
            final long position = this.position;
            byte val = backingBuffer.getByte(position);
            this.position = position + 1;
            return val;
        }

        public byte peekByte() {
            return backingBuffer.getByte(position);
        }

        public short getByteUnsigned() {
            final long position = this.position;
            short val = backingBuffer.getByteUnsigned(position);
            this.position = position + 1;
            return val;
        }

        public short peekByteUnsigned() {
            return backingBuffer.getByteUnsigned(position);
        }

        public short getShort() {
            final long position = this.position;
            short val = backingBuffer.getShort(position);
            this.position = position + 1;
            return val;
        }

        public short peekShort() {
            return backingBuffer.getShort(position);
        }

        public int getShortUnsigned() {
            final long position = this.position;
            int val = backingBuffer.getShortUnsigned(position);
            this.position = position + 1;
            return val;
        }

        public int peekShortUnsigned() {
            return backingBuffer.getShortUnsigned(position);
        }

        public int getInt() {
            final long position = this.position;
            int val = backingBuffer.getInt(position);
            this.position = position + 1;
            return val;
        }

        public int peekInt() {
            return backingBuffer.getInt(position);
        }

        public long getIntUnsigned() {
            final long position = this.position;
            long val = backingBuffer.getIntUnsigned(position);
            this.position = position + 1;
            return val;
        }

        public long peekIntUnsigned() {
            return backingBuffer.getIntUnsigned(position);
        }

        public long getLong() {
            final long position = this.position;
            long val = backingBuffer.getLong(position);
            this.position = position + 1;
            return val;
        }

        public long peekLong() {
            return backingBuffer.getLong(position);
        }

        public void getBytes(byte[] b, int off, int len) {
            final long position = this.position;
            backingBuffer.getBytes(position, b, off, len);
            this.position = position + len;
        }

        public void getBytes(byte[] b) {
            getBytes(b, 0, b.length);
        }

        public int getCodePoint() {
            final long position = this.position;
            final BinaryBuffer bb = backingBuffer;
            final int a = bb.getByteUnsigned(position);
            if (a < 0x80) {
                this.position = position + 1;
                return a;
            } else if (a < 0xC0 || 0xF8 <= a) {
                this.position = position + 1;
                return '�';
            } else {
                int b = bb.getByteUnsigned(position + 1);
                if (b < 0xC0 || 0xE0 <= b) {
                    this.position = position + 1;
                    return '�';
                } else if (a < 0xE0) {
                    this.position = position + 2;
                    return (a & 0b0001_1111) << 6 | b & 0b0011_1111;
                } else {
                    int c = bb.getByteUnsigned(position + 2);
                    if (c < 0xC0 || 0xE0 <= c) {
                        this.position = position + 1;
                        return '�';
                    } else if (a < 0xF0) {
                        this.position = position + 3;
                        return (a & 0b0000_1111) << 12 | (b & 0b0011_1111) << 6 | c & 0b0011_1111;
                    } else {
                        int d = bb.getByteUnsigned(position + 3);
                        if (d < 0xC0 || 0xE0 <= d) {
                            this.position = position + 1;
                            return '�';
                        } else {
                            this.position = position + 4;
                            return (a & 0b0000_0111) << 18 | (b & 0b0011_1111) << 12 | (c & 0b0011_1111) << 6 | d & 0b0011_1111;
                        }
                    }
                }
            }
        }

        public void close() {
        }
    }
}

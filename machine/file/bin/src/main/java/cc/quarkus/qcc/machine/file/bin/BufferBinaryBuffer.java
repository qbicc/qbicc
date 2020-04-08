package cc.quarkus.qcc.machine.file.bin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;

/**
 *
 */
class BufferBinaryBuffer implements BinaryBuffer {
    final ByteBuffer buf;

    BufferBinaryBuffer(final ByteBuffer buf) {
        this.buf = buf;
    }

    public ByteOrder getByteOrder() {
        return buf.order();
    }

    public ByteOrder setByteOrder(final ByteOrder newOrder) {
        final ByteOrder oldOrder = buf.order();
        buf.order(newOrder);
        return oldOrder;
    }

    public long size() {
        return buf.limit();
    }

    public long growBy(final long amount) {
        throw new UnsupportedOperationException();
    }

    public void growTo(final long newSize) {
        throw new UnsupportedOperationException();
    }

    public int getInt(final long offset) {
        return buf.getInt((int) offset);
    }

    public void putInt(final long offset, final int value) {
        buf.putInt((int) offset, value);
    }

    public short getShort(final long offset) {
        return buf.getShort((int) offset);
    }

    public void putShort(final long offset, final short value) {
        buf.putShort((int) offset, value);
    }

    public byte getByte(final long offset) {
        return buf.get((int) offset);
    }

    public void putByte(final long offset, final byte value) {
        buf.put((int) offset, value);
    }

    public long getLong(final long offset) {
        return buf.getLong((int) offset);
    }

    public void putLong(final long offset, final long value) {
        buf.putLong((int) offset, value);
    }

    public void getBytes(final long offset, final byte[] b, final int off, final int len) {
        buf.position((int) offset);
        try {
            buf.get(b, off, len);
        } finally {
            buf.position(0);
        }
    }

    public void putBytes(final long offset, final byte[] b, final int off, final int len) {
        buf.position((int) offset);
        try {
            buf.put(b, off, len);
        } finally {
            buf.position(0);
        }
    }

    public void putBytes(final long position, final BinaryBuffer buf, final long offset, final long size) {
        final ByteBuffer byteBuffer = this.buf;
        final int oldPos = byteBuffer.position();
        final int oldLim = byteBuffer.limit();
        byteBuffer.position((int) position);
        byteBuffer.limit((int) (position + size));
        try {
            buf.writeTo(offset, byteBuffer);
        } finally {
            byteBuffer.position(oldPos);
            byteBuffer.limit(oldLim);
        }
    }

    public void writeTo(final long position, final ByteBuffer targetBuf) {
        final ByteBuffer byteBuffer = this.buf;
        final int oldPos = byteBuffer.position();
        final int oldLim = byteBuffer.limit();
        byteBuffer.position((int) position);
        byteBuffer.limit((int) position + targetBuf.remaining());
        try {
            targetBuf.put(byteBuffer);
        } finally {
            byteBuffer.position(oldPos);
            byteBuffer.limit(oldLim);
        }
    }

    public void writeTo(final GatheringByteChannel channel) throws IOException {
        writeTo(channel, 0, size());
    }

    public void writeTo(final GatheringByteChannel channel, final long offset, final long cnt) throws IOException {
        final ByteBuffer b = buf.duplicate();
        b.position((int) offset);
        b.limit((int) cnt);
        while (b.hasRemaining()) {
            channel.write(b);
        }
    }

    public void close() {
    }
}

package org.qbicc.machine.file.bin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 */
class BufferBinaryRandomAccess extends BinaryRandomAccess {
    private final ByteBuffer buffer;
    private final ByteBuffer be;
    private final ByteBuffer le;

    BufferBinaryRandomAccess(ByteBuffer buffer) {
        this.buffer = buffer;
        be = buffer.duplicate().order(ByteOrder.BIG_ENDIAN);
        le = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);
    }

    ByteBuffer buffer() {
        return buffer;
    }

    @Override
    public ByteOrder order() {
        return buffer.order();
    }

    @Override
    public ByteOrder order(ByteOrder newOrder) {
        ByteOrder oldOrder = buffer.order();
        buffer.order(newOrder);
        return oldOrder;
    }

    @Override
    public BinaryInput reread(long offset, long size) {
        return new BufferBinaryInput(buffer.slice((int) offset, (int) size));
    }

    @Override
    public BinaryOutput rewrite(long offset, long size) {
        return new BufferBinaryOutput(buffer.slice((int) offset, (int) size));
    }

    @Override
    public int i8(long offset) {
        return buffer.get((int) offset);
    }

    @Override
    public void i8(long offset, int value) {
        buffer.put((int) offset, (byte) value);
    }

    @Override
    public byte[] i8ArrayGet(long offset, int size) {
        byte[] bytes = new byte[size];
        buffer.get((int) offset, bytes);
        return bytes;
    }

    @Override
    public void i8ArrayPut(long offset, byte[] bytes, int off, int len) {
        buffer.put((int) offset, bytes, off, len);
    }

    @Override
    public int i16(long offset) {
        return buffer.getShort((int) offset) & 0xffff;
    }

    @Override
    public int i16le(long offset) {
        return le.getShort((int) offset) & 0xffff;
    }

    @Override
    public int i16be(long offset) {
        return be.getShort((int) offset) & 0xffff;
    }

    @Override
    public void i16(long offset, int value) {
        buffer.putShort((int) offset, (short) value);
    }

    @Override
    public void i16le(long offset, int value) {
        le.putShort((int) offset, (short) value);
    }

    @Override
    public void i16be(long offset, int value) {
        be.putShort((int) offset, (short) value);
    }

    @Override
    public int i32(long offset) {
        return buffer.getInt((int) offset);
    }

    @Override
    public int i32le(long offset) {
        return le.getInt((int) offset);
    }

    @Override
    public int i32be(long offset) {
        return be.getInt((int) offset);
    }

    @Override
    public void i32(long offset, int value) {
        buffer.putInt((int) offset, value);
    }

    @Override
    public void i32le(long offset, int value) {
        le.putInt((int) offset, value);
    }

    @Override
    public void i32be(long offset, int value) {
        be.putInt((int) offset, value);
    }

    @Override
    public long i64(long offset) {
        return buffer.getLong((int) offset);
    }

    @Override
    public long i64le(long offset) {
        return le.getLong((int) offset);
    }

    @Override
    public long i64be(long offset) {
        return be.getLong((int) offset);
    }

    @Override
    public void i64(long offset, long value) {
        buffer.putLong((int) offset, value);
    }

    @Override
    public void i64le(long offset, long value) {
        le.putLong((int) offset, value);
    }

    @Override
    public void i64be(long offset, long value) {
        be.putLong((int) offset, value);
    }

    @Override
    public void close() throws IOException {
        // no operation
    }
}

package org.qbicc.machine.file.bin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 */
class BufferBinaryOutput extends BinaryOutput {
    final ByteBuffer buffer;

    BufferBinaryOutput(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public ByteOrder order() {
        return buffer.order();
    }

    @Override
    public ByteOrder order(final ByteOrder newOrder) {
        ByteOrder old = buffer.order();
        buffer.order(newOrder);
        return old;
    }

    @Override
    public void i8(int value) throws IOException {
        buffer.put((byte) value);
    }

    public void i8array(byte[] array, int off, int len) throws IOException {
        buffer.put(array, off, len);
    }

    public void i8buffer(ByteBuffer buffer) throws IOException {
        this.buffer.put(buffer);
    }

    @Override
    public void i16le(int value) throws IOException {
        if (order() == ByteOrder.LITTLE_ENDIAN) {
            i16(value);
        } else {
            i16(Short.reverseBytes((short) value));
        }
    }

    @Override
    public void i16be(int value) throws IOException {
        if (order() == ByteOrder.BIG_ENDIAN) {
            i16(value);
        } else {
            i16(Short.reverseBytes((short) value));
        }
    }

    @Override
    public void i16(int value) throws IOException {
        if (buffer.remaining() < 2) {
            super.i16(value);
        } else {
            buffer.putShort((short) value);
        }
    }

    @Override
    public void i32le(int value) throws IOException {
        if (order() == ByteOrder.LITTLE_ENDIAN) {
            i32(value);
        } else {
            i32(Integer.reverseBytes(value));
        }
    }

    @Override
    public void i32be(int value) throws IOException {
        if (order() == ByteOrder.BIG_ENDIAN) {
            i32(value);
        } else {
            i32(Integer.reverseBytes(value));
        }
    }

    @Override
    public void i32(int value) throws IOException {
        if (buffer.remaining() < 4) {
            super.i32(value);
        } else {
            buffer.putInt(value);
        }
    }

    @Override
    public void i64le(long value) throws IOException {
        if (order() == ByteOrder.LITTLE_ENDIAN) {
            i64(value);
        } else {
            i64(Long.reverseBytes(value));
        }
    }

    @Override
    public void i64be(long value) throws IOException {
        if (order() == ByteOrder.BIG_ENDIAN) {
            i64(value);
        } else {
            i64(Long.reverseBytes(value));
        }
    }

    @Override
    public void i64(long value) throws IOException {
        if (buffer.remaining() < 8) {
            super.i64(value);
        } else {
            buffer.putLong(value);
        }
    }

    @Override
    public void close() throws IOException {
    }
}

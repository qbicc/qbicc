package org.qbicc.machine.file.bin;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 *
 */
class BufferBinaryInput extends BinaryInput {
    private final ByteBuffer buffer;

    BufferBinaryInput(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    ByteBuffer buffer() {
        return buffer;
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
    public long position() {
        return buffer.position();
    }

    @Override
    public long remaining() {
        return buffer.remaining();
    }

    @Override
    public BinaryInput fork() throws IOException {
        return fork(order());
    }

    @Override
    public BinaryInput fork(ByteOrder order) throws IOException {
        ByteBuffer slice = buffer.slice();
        slice.order(order);
        return new BufferBinaryInput(slice);
    }

    @Override
    public BinaryInput fork(ByteOrder order, long size) throws IOException {
        if (size > buffer.remaining()) {
            return fork();
        }
        ByteBuffer slice = buffer.slice(buffer.position(), (int) size);
        slice.order(order);
        return new BufferBinaryInput(slice);
    }

    @Override
    public void skip(long amount) throws IOException {
        if (amount <= 0) {
            return;
        }
        if (amount > buffer.remaining()) {
            throw new EOFException();
        }
        buffer.position((int) (buffer.position() + amount));
    }

    @Override
    public int u8() throws IOException {
        try {
            return buffer.get() & 0xff;
        } catch (BufferUnderflowException ignored) {
            throw new EOFException();
        }
    }

    @Override
    public int u8peek() throws IOException {
        try {
            return buffer.get(buffer.position()) & 0xff;
        } catch (IndexOutOfBoundsException ignored) {
            throw new EOFException();
        }
    }

    @Override
    public int u8opt() throws IOException {
        return buffer.hasRemaining() ? u8() : -1;
    }

    @Override
    public int u8peekOpt() throws IOException {
        return buffer.hasRemaining() ? u8peek() : -1;
    }

    @Override
    public int u16() throws IOException {
        return s16() & 0xffff;
    }

    @Override
    public int u16le() throws IOException {
        return s16le() & 0xffff;
    }

    @Override
    public int u16be() throws IOException {
        return s16be() & 0xffff;
    }

    @Override
    public int s16() throws IOException {
        try {
            return buffer.getShort();
        } catch (BufferUnderflowException ignored) {
            throw new EOFException();
        }
    }

    @Override
    public int s16le() throws IOException {
        return order() == ByteOrder.LITTLE_ENDIAN ? s16() : Short.reverseBytes((short) s16());
    }

    @Override
    public int s16be() throws IOException {
        return order() == ByteOrder.BIG_ENDIAN ? s16() : Short.reverseBytes((short) s16());
    }

    @Override
    public int i32() throws IOException {
        try {
            return buffer.getInt();
        } catch (BufferUnderflowException ignored) {
            throw new EOFException();
        }
    }

    @Override
    public int i32le() throws IOException {
        return order() == ByteOrder.LITTLE_ENDIAN ? i32() : Integer.reverseBytes(i32());
    }

    @Override
    public int i32be() throws IOException {
        return order() == ByteOrder.BIG_ENDIAN ? i32() : Integer.reverseBytes(i32());
    }

    @Override
    public long i64() throws IOException {
        try {
            return buffer.getLong();
        } catch (BufferUnderflowException ignored) {
            throw new EOFException();
        }
    }

    @Override
    public long i64le() throws IOException {
        return order() == ByteOrder.LITTLE_ENDIAN ? i64() : Long.reverseBytes(i64());
    }

    @Override
    public long i64be() throws IOException {
        return order() == ByteOrder.BIG_ENDIAN ? i64() : Long.reverseBytes(i64());
    }

    @Override
    public void i8array(byte[] array, int off, int len) throws IOException {
        try {
            buffer.get(array, off, len);
        } catch (BufferUnderflowException ignored) {
            throw new EOFException();
        }
    }

    @Override
    public void transferTo(BinaryOutput bo) throws IOException {
        bo.i8buffer(buffer);
    }

    @Override
    public void transferTo(FileChannel fc) throws IOException {
        fc.write(buffer);
    }

    @Override
    public void transferTo(OutputStream os) throws IOException {
        if (buffer.hasArray()) {
            os.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
            buffer.position(buffer.limit());
        } else {
            super.transferTo(os);
        }
    }

    @Override
    public void i16array(short[] array, int off, int len) throws IOException {
        try {
            buffer.asShortBuffer().get(array, off, len);
            buffer.position(buffer.position() + len << 1);
        } catch (BufferUnderflowException ignored) {
            throw new EOFException();
        }
    }

    @Override
    public void i32array(int[] array, int off, int len) throws IOException {
        try {
            buffer.asIntBuffer().get(array, off, len);
            buffer.position(buffer.position() + len << 2);
        } catch (BufferUnderflowException ignored) {
            throw new EOFException();
        }
    }

    @Override
    public void i64array(long[] array, int off, int len) throws IOException {
        try {
            buffer.asLongBuffer().get(array, off, len);
            buffer.position(buffer.position() + len << 3);
        } catch (BufferUnderflowException ignored) {
            throw new EOFException();
        }
    }

    @Override
    public void close() throws IOException {
        // no operation
    }
}

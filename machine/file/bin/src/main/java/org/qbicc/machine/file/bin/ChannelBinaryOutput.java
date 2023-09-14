package org.qbicc.machine.file.bin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;

/**
 *
 */
abstract class ChannelBinaryOutput extends BinaryOutput {
    private final ByteBuffer buffer;

    ChannelBinaryOutput(ByteOrder order) {
        super();
        // todo: pooling, etc
        buffer = ByteBuffer.allocateDirect(8192);
        buffer.order(order);
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

    abstract WritableByteChannel channel() throws IOException;

    @Override
    public void i8(int value) throws IOException {
        if (buffer.hasRemaining()) {
            buffer.put((byte) value);
        } else {
            flush();
        }
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
    public void i8array(byte[] array, int off, int len) throws IOException {
        int rem = buffer.remaining();
        int cnt = Math.min(len, rem);
        buffer.put(array, off, cnt);
        while (len > 0) {
            flush();
            len -= cnt;
            off += cnt;
            rem = buffer.remaining();
            cnt = Math.min(len, rem);
            buffer.put(array, off, cnt);
        }
    }

    private static int copy(ByteBuffer to, ByteBuffer from) {
        int rem = to.remaining();
        int pos = from.position();
        int lim = from.limit();
        if (rem >= lim - pos) {
            // plenty of room
            to.put(from);
            return lim - pos;
        } else {
            // restrict to the space left in from
            from.limit(pos + rem);
            try {
                to.put(from);
            } finally {
                from.limit(lim);
            }
            return rem;
        }
    }

    @Override
    public void i8buffer(ByteBuffer buffer) throws IOException {
        int outPos = this.buffer.position();
        int outLim = this.buffer.limit();
        int inPos = buffer.position();
        int inLim = buffer.limit();
        if (inLim - inPos <= outLim - outPos) {
            // it does not overflow our buffer
            this.buffer.put(buffer);
            return;
        }
        if (buffer.isDirect() || ! this.buffer.isDirect()) {
            // avoid an extra copy; just flush what we've got and write the rest
            flush();
            while (buffer.hasRemaining()) {
                channel().write(buffer);
            }
        } else {
            // copy their heap buffer through our direct buffer
            copy(this.buffer, buffer);
            while (buffer.hasRemaining()) {
                flush();
                copy(this.buffer, buffer);
            }
        }
    }

    void flush() throws IOException {
        buffer.flip();
        try {
            while (buffer.hasRemaining()) {
                channel().write(buffer);
            }
        } finally {
            buffer.compact();
        }
    }

    ByteBuffer buffer() {
        return buffer;
    }

    @Override
    public void close() throws IOException {
        // do not flush; temporary buffers do not create the file unless the buffer fills up
    }
}

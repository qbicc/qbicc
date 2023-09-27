package org.qbicc.machine.file.bin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;

/**
 *
 */
abstract class ChannelBinaryOutput extends BufferBinaryOutput {
    ChannelBinaryOutput(ByteOrder order) {
        super(ByteBuffer.allocateDirect(8192));
        buffer.order(order);
    }

    abstract WritableByteChannel channel() throws IOException;

    @Override
    public void i8(int value) throws IOException {
        if (! buffer.hasRemaining()) {
            flush();
        }
        buffer.put((byte) value);
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

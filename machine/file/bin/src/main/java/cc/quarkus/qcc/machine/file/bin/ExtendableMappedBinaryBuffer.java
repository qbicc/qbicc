package org.qbicc.machine.file.bin;

import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 *
 */
class ExtendableMappedBinaryBuffer extends MappedBinaryBuffer {
    private static final ByteBuffer ONE_BYTE;

    static {
        final ByteBuffer buf = ByteBuffer.allocateDirect(1);
        buf.put((byte) 0);
        buf.flip();
        ONE_BYTE = buf;
    }

    private final FileChannel channel;

    ExtendableMappedBinaryBuffer(final FileChannel channel) throws IOException {
        super(channel.map(FileChannel.MapMode.READ_WRITE, 0, 0x1000_0000));
        this.channel = channel;
    }

    public long size() {
        try {
            return channel.size();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public long growBy(final long amount) {
        final long newSize = size() + amount;
        growTo(newSize);
        return newSize;
    }

    public void growTo(final long newSize) {
        try {
            channel.write(ONE_BYTE.duplicate(), newSize - 1);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void close() {
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }
}

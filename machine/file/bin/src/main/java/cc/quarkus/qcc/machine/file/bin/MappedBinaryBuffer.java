package cc.quarkus.qcc.machine.file.bin;

import java.nio.MappedByteBuffer;

class MappedBinaryBuffer extends BufferBinaryBuffer {

    MappedBinaryBuffer(final MappedByteBuffer buf) {
        super(buf);
    }

    public void close() {
        ((MappedByteBuffer) buf).force();
    }
}

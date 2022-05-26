package org.qbicc.machine.vio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

final class PipeSinkHandler implements WritableIoHandler {
    private final Pipe.SinkChannel sink;
    private final ByteBuffer singleByte = ByteBuffer.allocateDirect(1);

    PipeSinkHandler(final Pipe.SinkChannel sink) {
        this.sink = sink;
    }

    @Override
    public void close() throws IOException {
        sink.close();
    }

    @Override
    public boolean isAppend() {
        return true;
    }

    @Override
    public int write(ByteBuffer buf) throws IOException {
        return sink.write(buf);
    }

    @Override
    public void writeSingle(int value) throws IOException {
        ByteBuffer singleByte = this.singleByte;
        singleByte.position(0);
        singleByte.put(0, (byte) value);
        if (sink.write(singleByte) == 0) {
            throw new IOException("Would block");
        }
    }

    @Override
    public int append(ByteBuffer buf) throws IOException {
        return write(buf);
    }

    @Override
    public void appendSingle(int value) throws IOException {
        writeSingle(value);
    }
}

package org.qbicc.machine.vio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

final class PipeSourceHandler implements ReadableIoHandler {
    private final Pipe.SourceChannel source;
    private final ByteBuffer singleByte = ByteBuffer.allocateDirect(1);

    PipeSourceHandler(final Pipe.SourceChannel source) {
        this.source = source;
    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        return source.read(buf);
    }

    public int readSingle() throws IOException {
        ByteBuffer singleByte = this.singleByte;
        singleByte.position(0);
        int cnt = source.read(singleByte);
        return cnt == -1 ? -1 : singleByte.get(0);
    }

    public long available() {
        return 0;
    }
}

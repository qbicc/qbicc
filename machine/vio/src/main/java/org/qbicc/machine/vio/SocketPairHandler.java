package org.qbicc.machine.vio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;

final class SocketPairHandler implements ReadableIoHandler, WritableIoHandler, DuplexIoHandler {
    private final Pipe.SourceChannel source;
    private final Pipe.SinkChannel sink;
    private final ByteBuffer singleByte = ByteBuffer.allocateDirect(1);

    SocketPairHandler(Pipe.SourceChannel source, Pipe.SinkChannel sink) {
        this.source = source;
        this.sink = sink;
    }

    @Override
    public boolean isAppend() {
        return true;
    }

    @Override
    public void shutdownInput() throws IOException {
        source.close();
    }

    @Override
    public void shutdownOutput() throws IOException {
        sink.close();
    }

    @Override
    public void close() throws IOException {
        source.close();
        sink.close();
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        try {
            return source.read(buf);
        } catch (ClosedChannelException e) {
            return -1;
        }
    }

    @Override
    public int readSingle() throws IOException {
        ByteBuffer singleByte = this.singleByte;
        singleByte.position(0);
        int cnt = source.read(singleByte);
        return cnt == -1 ? -1 : singleByte.get(0);
    }

    @Override
    public long available() {
        return 0;
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

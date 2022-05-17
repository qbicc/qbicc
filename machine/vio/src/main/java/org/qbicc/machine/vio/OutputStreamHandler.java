package org.qbicc.machine.vio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 *
 */
final class OutputStreamHandler implements IoHandler, WritableIoHandler {
    private final OutputStream os;

    OutputStreamHandler(final OutputStream os) {
        this.os = os;
    }

    @Override
    public void close() throws IOException {
        os.close();
    }

    @Override
    public boolean isAppend() {
        return true;
    }

    @Override
    public int write(ByteBuffer buf) throws IOException {
        return Utils.writeFrom(os, buf);
    }

    public void writeSingle(final int value) throws IOException {
        os.write(value);
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

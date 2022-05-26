package org.qbicc.machine.vio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 *
 */
final class InputStreamHandler implements IoHandler, ReadableIoHandler {
    private final InputStream is;

    InputStreamHandler(final InputStream is) {
        this.is = is;
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        return Utils.readInto(is, buf);
    }

    @Override
    public int readSingle() throws IOException {
        return is.read();
    }

    @Override
    public long available() throws IOException {
        return is.available();
    }
}

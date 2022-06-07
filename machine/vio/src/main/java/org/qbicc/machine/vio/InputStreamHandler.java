package org.qbicc.machine.vio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 *
 */
class InputStreamHandler implements IoHandler, ReadableIoHandler, SeekableIoHandler {
    private final InputStream is;
    private long pos;

    InputStreamHandler(final InputStream is) {
        this.is = is;
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        int cnt = Utils.readInto(is, buf);
        if (cnt > 0) {
            pos += cnt;
        }
        return cnt;
    }

    @Override
    public int readSingle() throws IOException {
        return is.read();
    }

    @Override
    public long available() throws IOException {
        return is.available();
    }

    @Override
    public long seekRelative(long offs) throws IOException {
        if (offs == 0) {
            return pos;
        } else if (offs > 0) {
            is.skipNBytes(offs);
            pos += offs;
            return pos;
        } else {
            throw new IOException("Cannot seek backwards");
        }
    }

    @Override
    public long seekAbsolute(long offs) throws IOException {
        return seekRelative(offs - pos);
    }
}

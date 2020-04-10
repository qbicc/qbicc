package cc.quarkus.qcc.type;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 *
 */
final class ByteBufferInputStream extends InputStream {
    private final ByteBuffer buf;

    ByteBufferInputStream(final ByteBuffer buf) {
        this.buf = buf;
    }

    public int read() {
        return buf.hasRemaining() ? buf.get() & 0xff : -1;
    }

    public int read(final byte[] b, final int off, final int len) {
        final int rem = buf.remaining();
        if (rem == 0) {
            return -1;
        }
        int c = Math.min(len, rem);
        buf.get(b, off, c);
        return c;
    }

    public byte[] readAllBytes() {
        final byte[] b = new byte[buf.remaining()];
        buf.get(b);
        return b;
    }

    public long skip(final long n) {
        int c = (int) Math.min(n, buf.remaining());
        buf.position(buf.position() + c);
        return c;
    }

    public int available() {
        return buf.remaining();
    }

    public void mark(final int ignored) {
        buf.mark();
    }

    public void reset() {
        buf.reset();
    }

    public boolean markSupported() {
        return true;
    }

    public void close() {
        buf.position(buf.limit());
    }
}

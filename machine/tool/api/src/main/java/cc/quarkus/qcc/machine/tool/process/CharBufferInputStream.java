package cc.quarkus.qcc.machine.tool.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;

final class CharBufferInputStream extends InputStream {
    final ByteBuffer bb;
    private final CharBuffer buf;
    private final CharsetEncoder e;

    CharBufferInputStream(final CharBuffer buf, final CharsetEncoder e) {
        this.buf = buf;
        this.e = e;
        bb = ByteBuffer.allocate(256).limit(0);
    }

    public void close() {
        bb.limit(0);
        buf.limit(0);
    }

    public int available() {
        return bb.remaining();
    }

    public long transferTo(final OutputStream out) throws IOException {
        while (! bb.hasRemaining()) {
            if (! buf.hasRemaining()) {
                return 0;
            }
            fill();
        }
        int cnt = bb.remaining();
        out.write(bb.array(), bb.arrayOffset() + bb.position(), cnt);
        bb.limit(0);
        return cnt;
    }

    public int read() {
        while (! bb.hasRemaining()) {
            if (! buf.hasRemaining()) {
                return - 1;
            }
            fill();
        }
        return bb.get() & 0xff;
    }

    public int read(final byte[] b, final int off, final int len) {
        while (! bb.hasRemaining()) {
            if (! buf.hasRemaining()) {
                return - 1;
            }
            fill();
        }
        int cnt = Math.min(len, bb.remaining());
        bb.get(b, off, cnt);
        return cnt;
    }

    private void fill() {
        bb.clear();
        try {
            e.encode(buf, bb, true);
        } finally {
            bb.flip();
        }
    }

    public long skip(final long n) {
        long c = 0;
        long diff;
        diff = n - c;
        while (diff > 0) {
            if (bb.remaining() < diff) {
                bb.position(0);
                bb.limit(0);
                if (read() == - 1) {
                    return diff;
                }
                c++;
                diff = n - c;
            } else {
                bb.position((int) (bb.position() + diff));
                // skipped it all
                return n;
            }
        }
        return diff;
    }
}

package org.qbicc.machine.vio;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 *
 */
final class Utils {
    private Utils() {}

    static int readInto(InputStream is, ByteBuffer buffer) throws IOException {
        int cnt;
        if (buffer.hasArray()) {
            byte[] array = buffer.array();
            int pos = buffer.position();
            int offs = buffer.arrayOffset() + pos;
            int limit = buffer.limit();
            try {
                cnt = is.read(array, offs, limit - pos);
            } catch (InterruptedIOException iioe) {
                cnt = iioe.bytesTransferred;
            }
            if (cnt > 0) {
                buffer.position(pos + cnt);
            }
        } else {
            byte[] array = new byte[buffer.remaining()];
            try {
                cnt = is.read(array);
            } catch (InterruptedIOException iioe) {
                cnt = iioe.bytesTransferred;
            }
            if (cnt > 0) {
                buffer.put(array, 0, cnt);
            }
        }
        return cnt;
    }

    public static int writeFrom(OutputStream os, ByteBuffer buffer) throws IOException {
        int cnt;
        int pos = buffer.position();
        if (buffer.hasArray()) {
            byte[] array = buffer.array();
            int offs = buffer.arrayOffset() + pos;
            int limit = buffer.limit();
            cnt = limit - pos;
            try {
                os.write(array, offs, cnt);
            } catch (InterruptedIOException iioe) {
                cnt = iioe.bytesTransferred;
            }
            buffer.position(pos + cnt);
        } else {
            cnt = buffer.remaining();
            byte[] array = new byte[cnt];
            buffer.get(pos, array);
            try {
                os.write(array);
            } catch (InterruptedIOException iioe) {
                cnt = iioe.bytesTransferred;
            }
            buffer.position(pos + cnt);
        }
        return cnt;
    }
}

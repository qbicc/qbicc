package org.qbicc.machine.vio;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A handler which has a file offset and can be read from at a given position.
 */
public interface SeekableReadableIoHandler extends SeekableIoHandler, ReadableIoHandler {
    int pread(ByteBuffer buf, long position) throws IOException;
}

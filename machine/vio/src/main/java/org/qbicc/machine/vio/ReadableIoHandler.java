package org.qbicc.machine.vio;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A handler which can be read from.
 */
public interface ReadableIoHandler extends IoHandler {
    int read(ByteBuffer buf) throws IOException;

    int readSingle() throws IOException;

    long available() throws IOException;
}

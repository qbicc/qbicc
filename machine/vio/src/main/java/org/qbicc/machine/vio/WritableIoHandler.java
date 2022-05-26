package org.qbicc.machine.vio;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A handler which can be written to.
 */
public interface WritableIoHandler extends IoHandler {
    int write(ByteBuffer buf) throws IOException;

    int append(ByteBuffer buf) throws IOException;

    boolean isAppend() throws IOException;

    void writeSingle(int value) throws IOException;

    void appendSingle(int value) throws IOException;
}

package org.qbicc.machine.vio;

import java.io.IOException;

/**
 * An I/O handler for bidirectional/duplexed pipe-line things.
 */
public interface DuplexIoHandler extends IoHandler {
    void shutdownInput() throws IOException;
    void shutdownOutput() throws IOException;
}

package org.qbicc.machine.vio;

import java.io.Closeable;
import java.io.IOException;

/**
 * A handler for a given open file.
 */
public interface IoHandler extends Closeable {
    /**
     * Close the handler.
     * Should not be directly called.
     * Even if an exception is thrown, the underlying resource should be considered to be released.
     *
     * @throws IOException if the close failed
     */
    @Override
    void close() throws IOException;
}

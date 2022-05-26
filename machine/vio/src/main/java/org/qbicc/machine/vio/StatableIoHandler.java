package org.qbicc.machine.vio;

import java.io.IOException;

/**
 * A handler for a file that can have its status read.
 */
public interface StatableIoHandler extends IoHandler {
    /**
     * Get the size of the target.
     *
     * @return the target's size
     * @throws IOException if an error occurs
     */
    long getSize() throws IOException;
}

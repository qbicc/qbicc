package org.qbicc.machine.vio;

import java.io.IOException;

/**
 * A handler which has a file offset.
 */
public interface SeekableIoHandler extends IoHandler {
    /**
     * Seek to the given relative file position, and return the previous file position.
     *
     * @param offs the relative offset
     * @return the previous file offset
     */
    long seekRelative(long offs) throws IOException;

    /**
     * Seek to the given absolute file position, and return the previous file position.
     *
     * @param offs the absolute offset
     * @return the previous file offset
     */
    long seekAbsolute(long offs) throws IOException;
}

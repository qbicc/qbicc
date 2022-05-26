package org.qbicc.machine.vio;

import java.io.IOException;

/**
 * A handler for directory-like things.
 */
public interface DirectoryIoHandler extends IoHandler {
    String readEntry() throws IOException;
}

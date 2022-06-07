package org.qbicc.machine.vio;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

/**
 *
 */
class ZipEntryInputStreamHandler extends InputStreamHandler implements StatableIoHandler {
    private final ZipEntry ze;

    ZipEntryInputStreamHandler(ZipEntry ze, InputStream is) {
        super(is);
        this.ze = ze;
    }

    @Override
    public long getSize() throws IOException {
        return ze.getSize();
    }
}

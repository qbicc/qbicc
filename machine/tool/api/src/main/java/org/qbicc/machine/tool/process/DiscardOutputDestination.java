package org.qbicc.machine.tool.process;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

final class DiscardOutputDestination extends OutputDestination {

    static final OutputDestination INSTANCE = new DiscardOutputDestination();

    private DiscardOutputDestination() {}

    void transferFrom(final InputSource source) {
    }

    void transferFrom(final InputStream stream) throws IOException {
        while (stream.read() != -1) {
            stream.skip(Integer.MAX_VALUE);
        }
    }

    void transferFrom(final Reader reader, final Charset charset) throws IOException {
        while (reader.read() != -1) {
            reader.skip(Integer.MAX_VALUE);
        }
    }

    Closeable transferFromErrorOf(final Process process, final ProcessBuilder.Redirect redirect) {
        // already set to DISCARD
        return Closeables.BLANK_CLOSEABLE;
    }

    ProcessBuilder.Redirect getOutputRedirect() {
        return ProcessBuilder.Redirect.DISCARD;
    }
}

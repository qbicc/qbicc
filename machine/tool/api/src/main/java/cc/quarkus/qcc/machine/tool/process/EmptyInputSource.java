package cc.quarkus.qcc.machine.tool.process;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class EmptyInputSource extends InputSource {
    static final EmptyInputSource INSTANCE = new EmptyInputSource();

    private EmptyInputSource() {}

    public void transferTo(final OutputDestination destination) throws IOException {
        destination.transferFrom(this);
    }

    ProcessBuilder.Redirect getInputRedirect() {
        return ProcessBuilder.Redirect.DISCARD;
    }

    Closeable provideProcessInput(final Process process, final ProcessBuilder.Redirect inputRedirect) {
        return Closeables.BLANK_CLOSEABLE;
    }

    InputStream openStream() {
        return InputStream.nullInputStream();
    }

    void transferTo(final OutputStream os) {
    }

    void writeTo(final Path path) throws IOException {
        Files.copy(InputStream.nullInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
    }
}

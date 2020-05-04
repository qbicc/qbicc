package cc.quarkus.qcc.machine.tool.process;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 *
 */
final class PathInputSource extends InputSource {
    private final Path path;

    PathInputSource(final Path path) {
        this.path = path;
    }

    public void transferTo(final OutputDestination destination) throws IOException {
        destination.transferFrom(this);
    }

    ProcessBuilder.Redirect getInputRedirect() {
        if (path.getFileSystem() == FileSystems.getDefault()) {
            return ProcessBuilder.Redirect.from(path.toFile());
        } else {
            return ProcessBuilder.Redirect.PIPE;
        }
    }

    Closeable provideProcessInput(final Process process, final ProcessBuilder.Redirect inputRedirect) throws IOException {
        if (inputRedirect == ProcessBuilder.Redirect.PIPE) {
            return super.provideProcessInput(process, inputRedirect);
        } else {
            // handled directly
            return Closeables.BLANK_CLOSEABLE;
        }
    }

    void transferTo(final OutputStream os) throws IOException {
        Files.copy(path, os);
    }

    InputStream openStream() throws IOException {
        return Files.newInputStream(path);
    }

    void writeTo(final Path path) throws IOException {
        Files.copy(this.path, path, StandardCopyOption.REPLACE_EXISTING);
    }

    Path getPath() {
        return path;
    }
}

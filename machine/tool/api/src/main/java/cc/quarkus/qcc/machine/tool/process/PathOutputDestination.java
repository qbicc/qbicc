package cc.quarkus.qcc.machine.tool.process;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

final class PathOutputDestination extends OutputDestination {
    private final Path path;

    PathOutputDestination(final Path path) {
        this.path = path;
    }

    void transferFrom(final InputSource source) throws IOException {
        source.writeTo(path);
    }

    void transferFrom(final InputStream stream) throws IOException {
        Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
    }

    void transferFrom(final Reader reader, final Charset charset) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path, charset, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            reader.transferTo(w);
        }
    }

    Closeable transferFromErrorOf(final Process process, final ProcessBuilder.Redirect redirect) throws IOException {
        if (redirect == ProcessBuilder.Redirect.PIPE) {
            return super.transferFromErrorOf(process, redirect);
        } else {
            // already set to file
            return Closeables.BLANK_CLOSEABLE;
        }
    }

    void chain(final List<Process> processes, final List<ProcessBuilder> processBuilders, final int index) throws IOException {
        final ProcessBuilder processBuilder = processBuilders.get(index - 1);
        final Process process = processes.get(index - 1);
        if (processBuilder.redirectOutput() == ProcessBuilder.Redirect.PIPE) {
            transferFrom(process.getInputStream());
        } else {
            // do nothing (already piped)
        }
    }

    ProcessBuilder.Redirect getOutputRedirect() {
        if (path.getFileSystem() == FileSystems.getDefault()) {
            return ProcessBuilder.Redirect.to(path.toFile());
        } else {
            return ProcessBuilder.Redirect.PIPE;
        }
    }
}

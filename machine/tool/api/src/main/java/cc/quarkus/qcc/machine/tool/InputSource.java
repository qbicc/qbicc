package cc.quarkus.qcc.machine.tool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 */
public abstract class InputSource {
    InputSource() {
    }

    abstract InputStream open() throws IOException;

    public static final class File extends InputSource {
        final Path path;

        public File(final Path path) {
            this.path = path;
        }

        InputStream open() throws IOException {
            return Files.newInputStream(path);
        }
    }

    public static final class String extends InputSource {
        final java.lang.String string;

        public String(final java.lang.String string) {
            this.string = string;
        }

        InputStream open() {
            return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static final class Stream extends InputSource {
        final InputStream stream;

        public Stream(final InputStream stream) {
            this.stream = stream;
        }

        InputStream open() {
            return stream;
        }
    }

    public static final class Basic {
        private Basic() {
        }

        public static final InputSource EMPTY = new Stream(InputStream.nullInputStream());
    }

    void writeTo(Process process) throws IOException {
        try (OutputStream os = process.getOutputStream()) {
            try (InputStream is = open()) {
                is.transferTo(os);
            }
        }
    }
}

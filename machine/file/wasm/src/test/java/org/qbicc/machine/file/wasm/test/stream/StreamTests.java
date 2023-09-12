package org.qbicc.machine.file.wasm.test.stream;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.smallrye.common.function.ExceptionConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qbicc.machine.file.wasm.stream.ModuleReader;
import org.qbicc.machine.file.wasm.stream.ModuleVisitor;
import org.qbicc.machine.file.wasm.stream.ModuleWriter;

public class StreamTests {

    @Test
    public void testAllWasmFiles() throws URISyntaxException, IOException {
        // first locate them
        URL detected = StreamTests.class.getResource("/align.0.wasm");
        Assertions.assertNotNull(detected);
        Closeable c = null;
        Path p = switch (detected.getProtocol()) {
            case "file" -> Path.of(detected.getPath()).getParent();
            case "jar" -> {
                FileSystem fs = FileSystems.newFileSystem(detected.toURI(), Map.of());
                c = fs;
                yield fs.getPath("/");
            }
            default -> throw new IllegalStateException("Unsupported protocol for " + detected);
        };
        try {
            // now just process each one, in, out, and in again
            forEachWasmFile(p, path -> {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (ModuleReader wr = ModuleReader.forFile(path)) {
                    wr.accept(ModuleWriter.forStream(baos));
                }
                try (ModuleReader wr = ModuleReader.forBytes(baos.toByteArray())) {
                    wr.accept(new ModuleVisitor<>());
                }
            });
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private void forEachWasmFile(Path path, ExceptionConsumer<Path, IOException> handler) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
                for (Path p2 : ds) {
                    forEachWasmFile(p2, handler);
                }
            }
        } else if (path.getFileName().toString().endsWith(".wasm")){
            handler.accept(path);
        }

    }
}

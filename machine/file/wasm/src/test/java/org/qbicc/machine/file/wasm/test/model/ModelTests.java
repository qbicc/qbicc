package org.qbicc.machine.file.wasm.test.model;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteOrder;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.smallrye.common.function.ExceptionConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qbicc.machine.file.bin.BinaryInput;
import org.qbicc.machine.file.bin.BinaryOutput;
import org.qbicc.machine.file.wasm.model.Module;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

public class ModelTests {

    @Test
    public void testAllWasmFiles() throws URISyntaxException, IOException {
        // first locate them
        URL detected = ModelTests.class.getResource("/align.0.wasm");
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
            // now just process each one, in to model, out to stream, and in to model again
            forEachWasmFile(p, path -> {
                try (BinaryInput in = BinaryInput.open(path, ByteOrder.LITTLE_ENDIAN)) {
                    Module module = Module.loadFrom(in);
                    try (BinaryOutput out = BinaryOutput.temporary(Module::loadFrom)) {
                        module.saveTo(new WasmOutputStream(out));
                    }
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

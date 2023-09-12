package org.qbicc.machine.file.wasm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.smallrye.common.constraint.Assert;

/**
 * A representation of binary data stored somewhere.
 */
public abstract class Data {
    protected Data() {}

    public abstract InputStream openStream() throws IOException;

    public abstract void writeTo(OutputStream os) throws IOException;

    public byte[] asBytes() throws IOException {
        try (InputStream inputStream = openStream()) {
            return inputStream.readAllBytes();
        }
    }

    public static Data of(Path path) {
        Assert.checkNotNullParam("path", path);
        return new Data() {
            @Override
            public InputStream openStream() throws IOException {
                return Files.newInputStream(path);
            }

            @Override
            public void writeTo(OutputStream os) throws IOException {
                Files.copy(path, os);
            }

        };
    }

    public static Data of(byte[] bytes) {
        Assert.checkNotNullParam("bytes", bytes);
        return new Data() {
            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public void writeTo(OutputStream os) throws IOException {
                os.write(bytes);
            }

        };
    }
}

package cc.quarkus.qcc.machine.tool.process;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.nio.charset.Charset;

import io.smallrye.common.function.ExceptionBiConsumer;

final class ReaderConsumerOutputDestination<T> extends OutputDestination {
    private final ExceptionBiConsumer<T, Reader, IOException> consumer;
    private final T param;
    private final Charset charset;

    ReaderConsumerOutputDestination(final ExceptionBiConsumer<T, Reader, IOException> consumer, final T param, final Charset charset) {
        this.consumer = consumer;
        this.param = param;
        this.charset = charset;
    }

    void transferFrom(final InputStream stream) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(stream, charset)) {
            transferFrom(isr, charset);
        }
    }

    void transferFrom(final Reader reader, final Charset charset) throws IOException {
        consumer.accept(param, reader);
    }

    void transferFrom(final OutputStreamConsumerInputSource<?> source) throws IOException {
        // needs a separate thread to make it work
        try (PipedInputStream inputStream = new PipedInputStream()) {
            try (PipedOutputStream outputStream = new PipedOutputStream(inputStream)) {
                try (Closeable thr1 = Closeables.start("Transfer thread", () -> transferFrom(inputStream))) {
                    source.transferTo(outputStream);
                }
            }
        }
    }

    void transferFrom(final WriterConsumerInputSource<?> source) throws IOException {
        // needs a separate thread to make it work
        try (PipedReader reader = new PipedReader()) {
            try (PipedWriter writer = new PipedWriter(reader)) {
                try (Closeable thr1 = Closeables.start("Transfer thread", () -> transferFrom(reader, charset))) {
                    source.transferTo(writer, charset);
                }
            }
        }
    }

    ProcessBuilder.Redirect getOutputRedirect() {
        return ProcessBuilder.Redirect.PIPE;
    }
}

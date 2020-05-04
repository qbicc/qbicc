package cc.quarkus.qcc.machine.tool.process;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.nio.charset.Charset;

import io.smallrye.common.function.ExceptionBiConsumer;

final class InputStreamConsumerOutputDestination<T> extends OutputDestination {
    private final ExceptionBiConsumer<T, InputStream, IOException> consumer;
    private final T param;

    InputStreamConsumerOutputDestination(final ExceptionBiConsumer<T, InputStream, IOException> consumer, final T param) {
        this.consumer = consumer;
        this.param = param;
    }

    void transferFrom(final InputStream stream) throws IOException {
        consumer.accept(param, stream);
    }

    void transferFrom(final Reader reader, final Charset charset) throws IOException {
        // needs a separate thread to make it work
        try (PipedInputStream inputStream = new PipedInputStream()) {
            try (PipedOutputStream outputStream = new PipedOutputStream(inputStream)) {
                try (Closeable thr1 = Closeables.start("Transfer thread", () -> transferFrom(inputStream))) {
                    try (OutputStreamWriter osw = new OutputStreamWriter(outputStream, charset)) {
                        reader.transferTo(osw);
                    }
                }
            }
        }
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
        try (PipedInputStream inputStream = new PipedInputStream()) {
            try (PipedOutputStream outputStream = new PipedOutputStream(inputStream)) {
                try (Closeable thr1 = Closeables.start("Transfer thread", () -> transferFrom(inputStream))) {
                    source.transferTo(outputStream);
                }
            }
        }
    }

    ProcessBuilder.Redirect getOutputRedirect() {
        return ProcessBuilder.Redirect.PIPE;
    }
}

package cc.quarkus.qcc.machine.tool.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.ExceptionBiConsumer;

final class WriterConsumerInputSource<T> extends InputSource {
    private final ExceptionBiConsumer<T, Writer, IOException> consumer;
    private final T param;
    private final Charset charset;

    WriterConsumerInputSource(final ExceptionBiConsumer<T, Writer, IOException> consumer, final T param, final Charset charset) {
        this.consumer = consumer;
        this.param = param;
        this.charset = charset;
    }

    public void transferTo(final OutputDestination destination) throws IOException {
        destination.transferFrom(this);
    }

    ProcessBuilder.Redirect getInputRedirect() {
        return ProcessBuilder.Redirect.PIPE;
    }

    void transferTo(final OutputStream os) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(os, charset)) {
            transferTo(writer, charset);
        }
    }

    void transferTo(final Writer writer, final Charset charset) throws IOException {
        consumer.accept(param, writer);
    }

    InputStream openStream() {
        throw Assert.unsupported();
    }

    public String toString() {
        return "<stream>";
    }
}

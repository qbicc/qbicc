package cc.quarkus.qcc.machine.tool.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.ExceptionBiConsumer;

final class OutputStreamConsumerInputSource<T> extends InputSource {
    private final ExceptionBiConsumer<T, OutputStream, IOException> consumer;
    private final T param;

    OutputStreamConsumerInputSource(final ExceptionBiConsumer<T, OutputStream, IOException> consumer, final T param) {
        this.consumer = consumer;
        this.param = param;
    }

    public void transferTo(final OutputDestination destination) throws IOException {
        destination.transferFrom(this);
    }

    ProcessBuilder.Redirect getInputRedirect() {
        return ProcessBuilder.Redirect.PIPE;
    }

    void transferTo(final OutputStream os) throws IOException {
        consumer.accept(param, os);
    }

    InputStream openStream() {
        throw Assert.unsupported();
    }

    @Override
    public String toString() {
        return "<stream>";
    }
}

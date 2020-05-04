package cc.quarkus.qcc.machine.tool.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.Charset;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.ExceptionFunction;

final class ReaderSupplierInputSource<T> extends InputSource {
    private final ExceptionFunction<T, Reader, IOException> readerFactory;
    private final T param;
    private final Charset charset;

    ReaderSupplierInputSource(final ExceptionFunction<T, Reader, IOException> readerFactory, final T param, final Charset charset) {
        this.readerFactory = readerFactory;
        this.param = param;
        this.charset = charset;
    }

    public void transferTo(final OutputDestination destination) throws IOException {
        try (Reader reader = openReader()) {
            destination.transferFrom(reader, charset);
        }
    }

    ProcessBuilder.Redirect getInputRedirect() {
        return ProcessBuilder.Redirect.PIPE;
    }

    void transferTo(final OutputStream os) throws IOException {
        try (Reader reader = openReader()) {
            try (OutputStreamWriter writer = new OutputStreamWriter(os, charset)) {
                reader.transferTo(writer);
            }
        }
    }

    Reader openReader() throws IOException {
        return readerFactory.apply(param);
    }

    InputStream openStream() {
        throw Assert.unsupported();
    }
}

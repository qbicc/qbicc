package cc.quarkus.qcc.machine.tool.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.Charset;

import io.smallrye.common.function.ExceptionFunction;

final class OutputStreamSupplierOutputDestination<T> extends OutputDestination {
    private final ExceptionFunction<T, OutputStream, IOException> function;
    private final T param;

    OutputStreamSupplierOutputDestination(final ExceptionFunction<T, OutputStream, IOException> function, final T param) {
        this.function = function;
        this.param = param;
    }

    OutputStream openStream() throws IOException {
        return function.apply(param);
    }

    void transferFrom(final InputSource source) throws IOException {
        // some sources have more optimal ways to write to an output stream
        try (OutputStream os = openStream()) {
            source.transferTo(os);
        }
    }

    void transferFrom(final InputStream stream) throws IOException {
        try (OutputStream os = openStream()) {
            stream.transferTo(os);
        }
    }

    void transferFrom(final Reader reader, final Charset charset) throws IOException {
        try (OutputStream os = openStream()) {
            try (OutputStreamWriter osw = new OutputStreamWriter(os, charset)) {
                reader.transferTo(osw);
            }
        }
    }

    ProcessBuilder.Redirect getOutputRedirect() {
        return ProcessBuilder.Redirect.PIPE;
    }
}

package cc.quarkus.qcc.machine.tool.process;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import io.smallrye.common.function.ExceptionFunction;

final class AppendableSupplierOutputDestination<T> extends OutputDestination {
    private final ExceptionFunction<T, Appendable, IOException> supplier;
    private final T param;
    private final Charset charset;

    AppendableSupplierOutputDestination(final ExceptionFunction<T, Appendable, IOException> supplier, final T param, final Charset charset) {
        this.supplier = supplier;
        this.param = param;
        this.charset = charset;
    }

    void transferFrom(final InputStream is) throws IOException {
        try (InputStreamReader r = new InputStreamReader(is, charset)) {
            transferFrom(r, charset);
        }
    }

    void transferFrom(final Reader reader, final Charset charset) throws IOException {
        Appendable destination = openAppendable();
        if (destination instanceof Closeable) {
            try (Closeable closeable = (Closeable) destination) {
                if (destination instanceof Writer) {
                    reader.transferTo((Writer) destination);
                } else {
                    reader.transferTo(new AppendableWriter(destination));
                }
            }
        } else {
            reader.transferTo(new AppendableWriter(destination));
        }
    }

    Appendable openAppendable() throws IOException {
        return supplier.apply(param);
    }

    void transferFrom(final EmptyInputSource source) {
        // optimization
    }

    ProcessBuilder.Redirect getOutputRedirect() {
        return ProcessBuilder.Redirect.PIPE;
    }
}

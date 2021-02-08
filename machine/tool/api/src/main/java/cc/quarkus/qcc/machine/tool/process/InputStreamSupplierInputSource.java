package cc.quarkus.qcc.machine.tool.process;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import io.smallrye.common.function.ExceptionFunction;

final class InputStreamSupplierInputSource<T> extends InputSource {
    private final ExceptionFunction<T, InputStream, IOException> function;
    private final T param;

    InputStreamSupplierInputSource(final ExceptionFunction<T, InputStream, IOException> function, final T param) {
        this.function = function;
        this.param = param;
    }

    public void transferTo(final OutputDestination destination) throws IOException {
        destination.transferFrom(this);
    }

    ProcessBuilder.Redirect getInputRedirect() {
        return ProcessBuilder.Redirect.PIPE;
    }

    void writeTo(final Path path) throws IOException {
        try (InputStream is = openStream()) {
            Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public String toString() {
        return "<stream>";
    }

    ExceptionFunction<T, InputStream, IOException> getFunction() {
        return function;
    }

    T getParam() {
        return param;
    }

    InputStream openStream() throws IOException {
        return function.apply(param);
    }
}

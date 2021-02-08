package cc.quarkus.qcc.machine.tool.process;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.ExceptionBiConsumer;
import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.ExceptionFunction;
import io.smallrye.common.function.ExceptionSupplier;
import io.smallrye.common.function.Functions;

/**
 * A reusable source for input.
 */
public abstract class InputSource {
    InputSource() {}

    /**
     * An input source which is empty.
     *
     * @return the empty input source
     */
    public static InputSource empty() {
        return EmptyInputSource.INSTANCE;
    }

    /**
     * An input source which provides input from the given string, using the given
     * character set for byte-encoded destinations.
     *
     * @param string the string (must not be {@code null})
     * @param charset the character set (must not be {@code null})
     * @return the input source
     */
    public static InputSource from(CharSequence string, Charset charset) {
        Assert.checkNotNullParam("string", string);
        Assert.checkNotNullParam("charset", charset);
        return new CharSequenceInputSource(string, charset);
    }

    /**
     * An input source which provides input from the given string, using UTF-8
     * for byte-encoded destinations.
     *
     * @param string the string (must not be {@code null})
     * @return the input source
     */
    public static InputSource from(CharSequence string) {
        return from(string, StandardCharsets.UTF_8);
    }

    /**
     * An input source which provides input from a {@code Reader} provided by the given supplier,
     * using the given character set for byte-encoded destinations.
     *
     * @param supplier the reader supplier (must not be {@code null})
     * @param param the parameter to pass to the supplier
     * @param charset the character set (must not be {@code null})
     * @param <T> the type of the parameter
     * @return the input source
     */
    public static <T> InputSource from(ExceptionFunction<T, Reader, IOException> supplier, T param, Charset charset) {
        Assert.checkNotNullParam("supplier", supplier);
        Assert.checkNotNullParam("charset", charset);
        return new ReaderSupplierInputSource<T>(supplier, param, charset);
    }

    /**
     * An input source which provides input from a {@code Reader} provided by the given supplier,
     * using the given character set for byte-encoded destinations.
     *
     * @param supplier the reader supplier (must not be {@code null})
     * @param charset the character set (must not be {@code null})
     * @return the input source
     */
    public static InputSource from(ExceptionSupplier<Reader, IOException> supplier, Charset charset) {
        Assert.checkNotNullParam("supplier", supplier);
        return from(Functions.exceptionSupplierFunction(), supplier, charset);
    }

    /**
     * An input source which provides input from the output of the given consumer,
     * using the given character set for byte-encoded destinations.
     *
     * @param consumer the consumer (must not be {@code null})
     * @param param the parameter to pass to the consumer
     * @param charset the character set (must not be {@code null})
     * @param <T> the type of the parameter
     * @return the input source
     */
    public static <T> InputSource from(ExceptionBiConsumer<T, Writer, IOException> consumer, T param, Charset charset) {
        Assert.checkNotNullParam("consumer", consumer);
        Assert.checkNotNullParam("charset", charset);
        return new WriterConsumerInputSource<T>(consumer, param, charset);
    }

    /**
     * An input source which provides input from the output of the given consumer,
     * using the given character set for byte-encoded destinations.
     *
     * @param consumer the consumer (must not be {@code null})
     * @param charset the character set (must not be {@code null})
     * @return the input source
     */
    public static InputSource from(ExceptionConsumer<Writer, IOException> consumer, Charset charset) {
        Assert.checkNotNullParam("consumer", consumer);
        return from(Functions.exceptionConsumerBiConsumer(), consumer, charset);
    }

    /**
     * An input source which provides input from the given file path.
     *
     * @param path the source path (must not be {@code null})
     * @return the input source
     */
    public static InputSource from(Path path) {
        Assert.checkNotNullParam("path", path);
        return new PathInputSource(path);
    }

    /**
     * An input source which provides input from the given file path.
     *
     * @param file the source file (must not be {@code null})
     * @return the input source
     */
    public static InputSource from(File file) {
        Assert.checkNotNullParam("file", file);
        return from(file.toPath());
    }

    /**
     * An input source which provides input from the given supplier.
     *
     * @param supplier the input supplier (must not be {@code null})
     * @param param the parameter to pass to the supplier
     * @param <T> the type of the parameter
     * @return the input source
     */
    public static <T> InputSource from(final ExceptionFunction<T, InputStream, IOException> supplier, final T param) {
        Assert.checkNotNullParam("supplier", supplier);
        return new InputStreamSupplierInputSource<T>(supplier, param);
    }

    /**
     * An input source which provides input from the given supplier.
     *
     * @param supplier the input supplier (must not be {@code null})
     * @return the input source
     */
    public static InputSource from(final ExceptionSupplier<InputStream, IOException> supplier) {
        Assert.checkNotNullParam("supplier", supplier);
        return from(Functions.exceptionSupplierFunction(), supplier);
    }

    /**
     * An input source which provides input from the output of the given consumer.
     *
     * @param consumer the consumer (must not be {@code null})
     * @param param the parameter to pass to the consumer
     * @param <T> the type of the parameter
     * @return the input source
     */
    public static <T> InputSource from(final ExceptionBiConsumer<T, OutputStream, IOException> consumer, final T param) {
        Assert.checkNotNullParam("consumer", consumer);
        return new OutputStreamConsumerInputSource<T>(consumer, param);
    }

    /**
     * An input source which provides input from the output of the given consumer.
     *
     * @param consumer the consumer (must not be {@code null})
     * @return the input source
     */
    public static InputSource from(final ExceptionConsumer<OutputStream, IOException> consumer) {
        Assert.checkNotNullParam("consumer", consumer);
        return from(Functions.exceptionConsumerBiConsumer(), consumer);
    }

    /**
     * Execute a transfer of this source to the given destination.
     *
     * @param destination the destination to send this input to (must not be {@code null})
     * @throws IOException if the transfer fails completely or partially
     */
    public abstract void transferTo(OutputDestination destination) throws IOException;

    abstract ProcessBuilder.Redirect getInputRedirect();

    Closeable provideProcessInput(final Process process, final ProcessBuilder.Redirect inputRedirect) throws IOException {
        String name = "Input thread for process \"" + nameOf(process) + "\" (pid " + process.pid() + ")";
        return Closeables.start(name, () -> {
            try (OutputStream os = process.getOutputStream()) {
                transferTo(os);
            }
        });
    }

    void transferTo(OutputStream os) throws IOException {
        try (InputStream is = openStream()) {
            is.transferTo(os);
        }
    }

    abstract InputStream openStream() throws IOException;

    void writeTo(Path path) throws IOException {
        try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            transferTo(os);
        }
    }

    static String nameOf(final Process p) {
        return p.info().command().orElse("<unknown process>");
    }

    public abstract String toString();
}

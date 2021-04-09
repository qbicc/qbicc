package org.qbicc.machine.tool.process;

import static org.qbicc.machine.tool.process.InputSource.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.ExceptionBiConsumer;
import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.ExceptionFunction;
import io.smallrye.common.function.ExceptionSupplier;
import io.smallrye.common.function.ExceptionUnaryOperator;
import io.smallrye.common.function.Functions;

/**
 * A destination to send input to.
 *
 * @see InputSource#transferTo(OutputDestination)
 */
public abstract class OutputDestination {
    OutputDestination() {}

    /**
     * An output destination that efficiently discards the input.
     *
     * @return the output destination
     */
    public static OutputDestination discarding() {
        return DiscardOutputDestination.INSTANCE;
    }

    /**
     * An output destination that calls the given consumer with an input stream.
     *
     * @param consumer the stream consumer (must not be {@code null})
     * @param param the parameter to pass to the stream consumer
     * @param <T> the parameter type
     * @return the output destination
     */
    public static <T> OutputDestination of(ExceptionBiConsumer<T, InputStream, IOException> consumer, T param) {
        Assert.checkNotNullParam("consumer", consumer);
        return new InputStreamConsumerOutputDestination<T>(consumer, param);
    }

    /**
     * An output destination that calls the given consumer with an input stream.
     *
     * @param consumer the stream consumer (must not be {@code null})
     * @return the output destination
     */
    public static OutputDestination of(ExceptionConsumer<InputStream, IOException> consumer) {
        Assert.checkNotNullParam("consumer", consumer);
        return of(Functions.exceptionConsumerBiConsumer(), consumer);
    }

    /**
     * An output destination that writes to an output stream provided by the given supplier.
     *
     * @param supplier the stream supplier (must not be {@code null})
     * @param param the parameter to pass to the stream supplier
     * @param <T> the parameter type
     * @return the output destination
     */
    public static <T> OutputDestination of(ExceptionFunction<T, OutputStream, IOException> supplier, T param) {
        Assert.checkNotNullParam("supplier", supplier);
        return new OutputStreamSupplierOutputDestination<T>(supplier, param);
    }

    /**
     * An output destination that writes to an output stream provided by the given supplier.
     *
     * @param supplier the stream supplier (must not be {@code null})
     * @return the output destination
     */
    public static OutputDestination of(ExceptionSupplier<OutputStream, IOException> supplier) {
        Assert.checkNotNullParam("supplier", supplier);
        return of(Functions.exceptionSupplierFunction(), supplier);
    }


    /**
     * An output destination that calls the given consumer with a reader.
     *
     * @param consumer the stream consumer (must not be {@code null})
     * @param param the parameter to pass to the stream consumer
     * @param <T> the parameter type
     * @param charset the character set (must not be {@code null})
     * @return the output destination
     */
    public static <T> OutputDestination of(ExceptionBiConsumer<T, Reader, IOException> consumer, T param, Charset charset) {
        Assert.checkNotNullParam("consumer", consumer);
        Assert.checkNotNullParam("charset", charset);
        return new ReaderConsumerOutputDestination<T>(consumer, param, charset);
    }

    /**
     * An output destination that calls the given consumer with a reader.
     *
     * @param consumer the stream consumer (must not be {@code null})
     * @param charset the character set (must not be {@code null})
     * @return the output destination
     */
    public static OutputDestination of(ExceptionConsumer<Reader, IOException> consumer, Charset charset) {
        Assert.checkNotNullParam("consumer", consumer);
        Assert.checkNotNullParam("charset", charset);
        return of(Functions.exceptionConsumerBiConsumer(), consumer, charset);
    }

    /**
     * An output destination that writes to an appendable provided by the given supplier.  The appendable
     * will be closed if it implements {@link Closeable}.
     *
     * @param supplier the supplier (must not be {@code null})
     * @param param the parameter to pass to the stream supplier
     * @param charset the character set (must not be {@code null})
     * @param <T> the parameter type
     * @return the output destination
     */
    public static <T> OutputDestination of(ExceptionFunction<T, Appendable, IOException> supplier, T param, Charset charset) {
        Assert.checkNotNullParam("supplier", supplier);
        Assert.checkNotNullParam("charset", charset);
        return new AppendableSupplierOutputDestination<T>(supplier, param, charset);
    }

    /**
     * An output destination that writes to an appendable provided by the given supplier.  The appendable
     * will be closed if it implements {@link Closeable}.
     *
     * @param supplier the stream supplier (must not be {@code null})
     * @param charset the character set (must not be {@code null})
     * @return the output destination
     */
    public static OutputDestination of(ExceptionSupplier<Appendable, IOException> supplier, Charset charset) {
        Assert.checkNotNullParam("supplier", supplier);
        Assert.checkNotNullParam("charset", charset);
        return of(Functions.exceptionSupplierFunction(), supplier, charset);
    }

    /**
     * An output destination that writes to a file on the filesystem.
     *
     * @param path the path to write to (must not be {@code null})
     * @return the output destination
     */
    public static OutputDestination of(Path path) {
        Assert.checkNotNullParam("path", path);
        return new PathOutputDestination(path);
    }

    /**
     * An output destination that writes to the given {@code Appendable}.  Note that the destination
     * is not reusable.
     *
     * @param appendable the appendable to write to
     * @param charset the character set (must not be {@code null})
     * @return the output destination
     */
    public static OutputDestination of(Appendable appendable, Charset charset) {
        Assert.checkNotNullParam("appendable", appendable);
        return of(ExceptionUnaryOperator.identity(), appendable, charset);
    }

    /**
     * An output destination that writes to the given {@code Appendable}.  Note that the destination
     * is not reusable.  The UTF-8 character set is assumed.
     *
     * @param appendable the appendable to write to
     * @return the output destination
     */
    public static OutputDestination of(Appendable appendable) {
        return of(appendable, StandardCharsets.UTF_8);
    }

    /**
     * An output destination that pipes into a process.  The process builder's redirection settings will be overwritten.
     *
     * @param supplier the process builder supplier (must not be {@code null})
     * @param param the parameter to pass to the process builder supplier
     * @param errorDest the output destination for the process error stream (must not be {@code null})
     * @param outputDest the output destination for the process output stream (must not be {@code null})
     * @param <T> the parameter type
     * @return the output destination
     */
    public static <T> OutputDestination of(ExceptionFunction<T, ProcessBuilder, IOException> supplier, T param, OutputDestination errorDest, OutputDestination outputDest) {
        return of(supplier, param, errorDest, outputDest, ProcessOutputDestination.DEFAULT_CHECKER);
    }

    /**
     * An output destination that pipes into a process.  The process builder's redirection settings will be overwritten.
     *
     * @param <T> the parameter type
     * @param supplier the process builder supplier (must not be {@code null})
     * @param param the parameter to pass to the process builder supplier
     * @param errorDest the output destination for the process error stream (must not be {@code null})
     * @param outputDest the output destination for the process output stream (must not be {@code null})
     * @param checker the process exit code checker to use (must not be {@code null})
     * @return the output destination
     */
    public static <T> OutputDestination of(ExceptionFunction<T, ProcessBuilder, IOException> supplier, T param, OutputDestination errorDest, OutputDestination outputDest, final ExceptionConsumer<Process, IOException> checker) {
        Assert.checkNotNullParam("supplier", supplier);
        Assert.checkNotNullParam("errorDest", errorDest);
        Assert.checkNotNullParam("outputDest", outputDest);
        Assert.checkNotNullParam("checker", checker);
        return new ProcessOutputDestination<T>(supplier, param, errorDest, outputDest, checker);
    }

    /**
     * An output destination that pipes into a process.  The process builder's redirection settings will be overwritten.
     *
     * @param supplier the process builder supplier (must not be {@code null})
     * @param errorDest the output destination for the process error stream (must not be {@code null})
     * @param outputDest the output destination for the process output stream (must not be {@code null})
     * @return the output destination
     */
    public static OutputDestination of(ExceptionSupplier<ProcessBuilder, IOException> supplier, OutputDestination errorDest, OutputDestination outputDest) {
        Assert.checkNotNullParam("supplier", supplier);
        return of(Functions.exceptionSupplierFunction(), supplier, errorDest, outputDest);
    }

    /**
     * An output destination that pipes into a process.  The process builder's redirection settings will be overwritten.
     *
     * @param supplier the process builder supplier (must not be {@code null})
     * @param errorDest the output destination for the process error stream (must not be {@code null})
     * @param outputDest the output destination for the process output stream (must not be {@code null})
     * @param checker the process exit code checker to use (must not be {@code null})
     * @return the output destination
     */
    public static OutputDestination of(ExceptionSupplier<ProcessBuilder, IOException> supplier, OutputDestination errorDest, OutputDestination outputDest, final ExceptionConsumer<Process, IOException> checker) {
        Assert.checkNotNullParam("supplier", supplier);
        return of(Functions.exceptionSupplierFunction(), supplier, errorDest, outputDest, checker);
    }

    /**
     * An output destination that pipes into a process.  The process builder's redirection settings will be overwritten.
     * Note that the returned output destination reuses the same process builder and thus is <em>not</em> thread safe.
     *
     * @param processBuilder the process builder, which will be reused on every usage (must not be {@code null})
     * @param errorDest the output destination for the process error stream (must not be {@code null})
     * @param outputDest the output destination for the process output stream (must not be {@code null})
     * @return the output destination
     */
    public static OutputDestination of(ProcessBuilder processBuilder, OutputDestination errorDest, OutputDestination outputDest) {
        Assert.checkNotNullParam("processBuilder", processBuilder);
        return of(ExceptionUnaryOperator.identity(), processBuilder, errorDest, outputDest);
    }

    /**
     * An output destination that pipes into a process.  The process builder's redirection settings will be overwritten.
     * Note that the returned output destination reuses the same process builder and thus is <em>not</em> thread safe.
     *
     * @param processBuilder the process builder, which will be reused on every usage (must not be {@code null})
     * @param errorDest the output destination for the process error stream (must not be {@code null})
     * @param outputDest the output destination for the process output stream (must not be {@code null})
     * @param checker the process exit code checker to use (must not be {@code null})
     * @return the output destination
     */
    public static OutputDestination of(ProcessBuilder processBuilder, OutputDestination errorDest, OutputDestination outputDest, final ExceptionConsumer<Process, IOException> checker) {
        Assert.checkNotNullParam("processBuilder", processBuilder);
        return of(ExceptionUnaryOperator.identity(), processBuilder, errorDest, outputDest, checker);
    }

    void transferFrom(final InputSource source) throws IOException {
        try (InputStream is = source.openStream()) {
            transferFrom(is);
        }
    }

    void transferFrom(EmptyInputSource source) throws IOException {
        transferFrom((InputSource) source);
    }

    void transferFrom(PathInputSource source) throws IOException {
        transferFrom((InputSource) source);
    }

    void transferFrom(InputStreamSupplierInputSource<?> source) throws IOException {
        transferFrom((InputSource) source);
    }

    void transferFrom(OutputStreamConsumerInputSource<?> source) throws IOException {
        transferFrom((InputSource) source);
    }

    void transferFrom(ReaderSupplierInputSource<?> source) throws IOException {
        transferFrom((InputSource) source);
    }

    void transferFrom(WriterConsumerInputSource<?> source) throws IOException {
        transferFrom((InputSource) source);
    }

    abstract void transferFrom(InputStream stream) throws IOException;

    abstract void transferFrom(Reader reader, Charset charset) throws IOException;

    Closeable transferFromErrorOf(Process process, ProcessBuilder.Redirect redirect) throws IOException {
        String name = "Error thread for process \"" + nameOf(process) + "\" (pid " + process.pid() + ")";
        return Closeables.start(name, () -> {
            try (InputStream errorStream = process.getErrorStream()) {
                transferFrom(errorStream);
            }
        });
    }

    abstract ProcessBuilder.Redirect getOutputRedirect();

    List<ProcessBuilder> getBuilderPipeline(final ProcessBuilder.Redirect inputRedirect, int index) throws IOException {
        return Arrays.asList(new ProcessBuilder[index]);
    }

    void chain(List<Process> processes, List<ProcessBuilder> processBuilders, int index) throws IOException {
        transferFrom(processes.get(index - 1).getInputStream());
    }
}

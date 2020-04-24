package cc.quarkus.qcc.machine.tool.process;

import static cc.quarkus.qcc.machine.tool.process.ChainingProcessBuilder.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import io.smallrye.common.function.ExceptionBiConsumer;
import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.ExceptionFunction;
import io.smallrye.common.function.ExceptionSupplier;
import io.smallrye.common.function.Functions;

/**
 *
 */
public class Processes {
    private Processes() {}

    public static final ProcessOutputHandler DISCARD = new ProcessOutputHandler() {
        void handleOutput(final ProcessBuilder process) {
            process.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        }

        void handleError(final ProcessBuilder process) {
            process.redirectError(ProcessBuilder.Redirect.DISCARD);
        }
    };

    public static final ProcessInputProvider EMPTY = new ProcessInputProvider() {
        void provideInput(final ProcessBuilder firstProcess) {
            firstProcess.redirectInput(ProcessBuilder.Redirect.DISCARD);
        }
    };

    /**
     * Create a process input provider that reads the process input from a stream provided by the given function.  The
     * stream will be closed at the end of the stream input.
     *
     * @param argument the argument to pass to the function
     * @param streamSupplier the function which provides the input stream to read from
     * @param <T> the argument type
     * @return the input provider
     */
    public static <T> ProcessInputProvider inputStreamInputProvider(T argument, ExceptionFunction<T, InputStream, IOException> streamSupplier) {
        return outputStreamInputProvider(os -> {
            try (InputStream is = streamSupplier.apply(argument)) {
                is.transferTo(os);
            }
        });
    }

    /**
     * Create a process input provider that reads the process input from a stream provided by the given function.  The
     * stream will be closed at the end of the stream input.
     *
     * @param streamSupplier the supplier which provides the input stream to read from
     * @return the input provider
     */
    public static ProcessInputProvider inputStreamInputProvider(ExceptionSupplier<InputStream, IOException> streamSupplier) {
        return inputStreamInputProvider(streamSupplier, Functions.exceptionSupplierFunction());
    }

    /**
     * Create a process input provider that hands the output stream feeding the process input to the given consumer,
     * which will run in a dedicated thread.
     *
     * @param argument the argument to pass to the consumer
     * @param streamConsumer the consumer which accepts the stream
     * @param <T> the argument type
     * @return the input provider
     */
    public static <T> ProcessInputProvider outputStreamInputProvider(T argument, ExceptionBiConsumer<T, OutputStream, IOException> streamConsumer) {
        return new ProcessInputProvider() {
            void provideInput(final ProcessBuilder firstProcess) {
                firstProcess.redirectInput(ProcessBuilder.Redirect.PIPE);
            }

            Closeable getInputHandler(final Process process) throws IOException {
                String name = "Input stream handler thread for " + nameOf(process);
                return new ThreadCloseable(new CloseableThread(name) {
                    void runWithException() throws IOException {
                        streamConsumer.accept(argument, process.getOutputStream());
                    }
                });
            }
        };
    }

    /**
     * Create a process input provider that hands the output stream feeding the process input to the given consumer,
     * which will run in a dedicated thread.
     *
     * @param streamConsumer the consumer which accepts the stream
     * @return the input provider
     */
    public static ProcessInputProvider outputStreamInputProvider(ExceptionConsumer<OutputStream, IOException> streamConsumer) {
        return outputStreamInputProvider(streamConsumer, Functions.exceptionConsumerBiConsumer());
    }

    /**
     * Create a process output handler that hands the input stream containing the process output to the given consumer,
     * which will run in a dedicated thread.
     *
     * @param argument the argument to pass to the consumer
     * @param streamConsumer the consumer which accepts the stream
     * @param <T> the argument type
     * @return the output handler
     */
    public static <T> ProcessOutputHandler inputStreamOutputHandler(T argument, ExceptionBiConsumer<T, InputStream, IOException> streamConsumer) {
        return new ProcessOutputHandler() {
            void handleOutput(final ProcessBuilder process) {
                process.redirectOutput(ProcessBuilder.Redirect.PIPE);
            }

            void handleError(final ProcessBuilder process) {
                process.redirectError(ProcessBuilder.Redirect.PIPE);
            }

            Closeable getErrorHandler(final Process process) throws IOException {
                return getHandler(process, process.getInputStream(), "output");
            }

            Closeable getOutputHandler(final Process process) throws IOException {
                return getHandler(process, process.getErrorStream(), "error");
            }

            Closeable getHandler(Process process, InputStream stream, String desc) throws IOException {
                String name = desc + " stream handler thread for " + nameOf(process);
                return new ThreadCloseable(new CloseableThread(name) {
                    void runWithException() throws IOException {
                        streamConsumer.accept(argument, stream);
                    }
                });
            }
        };
    }

    /**
     * Create a process output handler that hands the input stream containing the process output to the given consumer,
     * which will run in a dedicated thread.
     *
     * @param streamConsumer the consumer which accepts the stream
     * @return the output handler
     */
    public static ProcessOutputHandler inputStreamOutputHandler(ExceptionConsumer<InputStream, IOException> streamConsumer) {
        return inputStreamOutputHandler(streamConsumer, Functions.exceptionConsumerBiConsumer());
    }

    /**
     * Create a process output handler that writes the process output to a stream provided by the given function.  The
     * stream will be closed at the end of the process output.
     *
     * @param argument the argument to pass to the function
     * @param streamFunction the function which provides the output stream to write to
     * @param <T> the argument type
     * @return the output handler
     */
    public static <T> ProcessOutputHandler outputStreamOutputHandler(T argument, ExceptionFunction<T, OutputStream, IOException> streamFunction) {
        return inputStreamOutputHandler(is -> {
            try (OutputStream os = streamFunction.apply(argument)) {
                is.transferTo(os);
            }
        });
    }

    /**
     * Create a process output handler that writes the process output to a stream provided by the given function.  The
     * stream will be closed at the end of the process output.
     *
     * @param streamSupplier the supplier which provides the output stream to write to
     * @return the output handler
     */
    public static ProcessOutputHandler outputStreamOutputHandler(ExceptionSupplier<OutputStream, IOException> streamSupplier) {
        return outputStreamOutputHandler(streamSupplier, Functions.exceptionSupplierFunction());
    }

    public static ProcessInputProvider inputFromFile(Path path) {
        return new ProcessInputProvider() {
            void provideInput(final ProcessBuilder firstProcess) {
                firstProcess.redirectInput(path.toFile());
            }
        };
    }

    public static ProcessOutputHandler outputToFile(Path path) {
        return new ProcessOutputHandler() {
            void handleError(final ProcessBuilder process) {
                process.redirectError(path.toFile());
            }

            void handleOutput(final ProcessBuilder process) {
                process.redirectOutput(path.toFile());
            }
        };
    }
}

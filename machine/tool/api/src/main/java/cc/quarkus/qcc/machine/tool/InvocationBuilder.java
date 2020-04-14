package cc.quarkus.qcc.machine.tool;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import cc.quarkus.qcc.context.Context;
import io.smallrye.common.function.ExceptionBiConsumer;

/**
 *
 */
public abstract class InvocationBuilder<P, R> {
    private final Tool tool;

    private InputSource inputSource = InputSource.Basic.EMPTY;

    protected InvocationBuilder(final Tool tool) {
        this.tool = tool;
    }

    public Tool getTool() {
        return tool;
    }

    protected InputSource getInputSource() {
        return inputSource;
    }

    protected InvocationBuilder<P, R> setInputSource(final InputSource inputSource) {
        this.inputSource = inputSource;
        return this;
    }

    /**
     * Invoke the tool.
     *
     * @return the invocation result
     */
    public R invoke() throws ToolExecutionFailureException {
        boolean intr = Thread.interrupted();
        if (intr) {
            Thread.currentThread().interrupt();
            throw new ToolExecutionFailureException("Execution interrupted");
        }
        final P param;
        try {
            param = createCollectorParam();
        } catch (ToolExecutionFailureException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new ToolExecutionFailureException("Tool setup failed", t);
        }
        final InputSource inputSource = this.inputSource;
        final ProcessBuilder pb = createProcessBuilder(param);
        pb.redirectInput(ProcessBuilder.Redirect.PIPE);
        Context.debug(null, ">>> %s", String.join(" ", pb.command()));
        final Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new ToolExecutionFailureException("Failed to start process", e);
        }
        try (ProcessCloseable ignored1 = new ProcessCloseable(process)) {
            // start output collectors
            try (CollectorThread<P> ignored2 = new CollectorThread<>(Context.requireCurrent(), param,
                    process.getInputStream(), this::collectOutput)) {
                try (CollectorThread<P> ignored3 = new CollectorThread<>(Context.requireCurrent(), param,
                        process.getErrorStream(), this::collectError)) {
                    try {
                        inputSource.writeTo(process);
                    } catch (IOException e) {
                        Context.error(null, "Failed to transfer input to process: %s", e);
                    }
                }
            }
        }
        try {
            return produceResult(param, process);
        } catch (ToolExecutionFailureException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new ToolExecutionFailureException("Tool output processing failed", t);
        }
    }

    protected int waitForProcessUninterruptibly(Process p) {
        boolean intr = Thread.interrupted();
        try {
            for (;;) {
                try {
                    return p.waitFor();
                } catch (InterruptedException e) {
                    intr = true;
                }
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected void collectOutput(P param, InputStream stream) throws Exception {
        while (stream.read() != -1)
            ;
    }

    protected void collectError(P param, InputStream stream) throws Exception {
        while (stream.read() != -1)
            ;
    }

    protected abstract R produceResult(P param, Process process) throws Exception;

    protected P createCollectorParam() throws Exception {
        // override in subclass if desired
        return null;
    }

    protected ProcessBuilder createProcessBuilder(P param) {
        final ProcessBuilder pb = new ProcessBuilder();
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        pb.command().add(getTool().getExecutablePath().toString());
        return pb;
    }

    static final class CollectorThread<P> extends Thread implements AutoCloseable {
        private final Context dc;
        private final P param;
        private final InputStream inputStream;
        private final ExceptionBiConsumer<P, InputStream, Exception> collector;
        Throwable problem;

        CollectorThread(final Context dc, final P param, final InputStream inputStream,
                final ExceptionBiConsumer<P, InputStream, Exception> collector) {
            super("Process output collector");
            this.dc = dc;
            this.param = param;
            this.inputStream = inputStream;
            this.collector = collector;
            start();
        }

        public void run() {
            try (Closeable ignored = inputStream) {
                dc.run(collector, param, inputStream);
            } catch (Throwable t) {
                problem = t;
            }
        }

        public void close() throws ToolExecutionFailureException {
            boolean intr = Thread.interrupted();
            try {
                for (;;)
                    try {
                        interrupt();
                        join();
                        final Throwable problem = this.problem;
                        if (problem != null) {
                            try {
                                throw problem;
                            } catch (ToolExecutionFailureException | RuntimeException | Error e) {
                                throw e;
                            } catch (Throwable t) {
                                throw new ToolExecutionFailureException("Tool output collection failed", t);
                            }
                        }
                        return;
                    } catch (InterruptedException ignored) {
                        intr = true;
                    }
            } finally {
                if (intr) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    static final class ProcessCloseable implements AutoCloseable {
        private final Process process;

        ProcessCloseable(Process process) {
            this.process = process;
        }

        public void close() {
            boolean intr = Thread.interrupted();
            if (intr) {
                process.destroyForcibly();
            } else {
                process.destroy();
            }
            try {
                for (;;)
                    try {
                        process.waitFor();
                        return;
                    } catch (InterruptedException ignored) {
                        process.destroyForcibly();
                        intr = true;
                    }
            } finally {
                if (intr) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}

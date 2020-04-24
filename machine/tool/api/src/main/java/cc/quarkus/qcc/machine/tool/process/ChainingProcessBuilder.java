package cc.quarkus.qcc.machine.tool.process;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.smallrye.common.constraint.Assert;

/**
 * A process builder, which runs a process and can also act as the destination for another process' output.
 */
public class ChainingProcessBuilder extends ProcessChainTarget {
    private final ProcessBuilder processBuilder;
    private final List<String> command = new ArrayList<>(0);

    private final ProcessChainTarget output;
    private final ProcessOutputHandler error;
    private ProcessStatusChecker checker = p -> {
        if (p.exitValue() != 0) throw new IOException("Process returned exit code " + p.exitValue());
    };

    ChainingProcessBuilder(final String command, final ProcessOutputHandler error, final ProcessChainTarget output) {
        processBuilder = new ProcessBuilder();
        this.command.add(command);
        this.output = output;
        this.error = error;
    }

    /**
     * Create a new chaining process builder.  If the given chain target is another chaining process builder, then
     * starting this process will cause that process to also execute, with the output of this process feeding the input
     * of that process.
     *
     * @param command the command to run (must not be {@code null})
     * @param error   the output handler for the error stream (must not be {@code null})
     * @param output  the output handler or chaining target for the output stream (must not be {@code null})
     * @return the new process builder
     */
    public static ChainingProcessBuilder create(String command, ProcessOutputHandler error, ProcessChainTarget output) {
        Assert.checkNotNullParam("command", command);
        Assert.checkNotNullParam("error", error);
        Assert.checkNotNullParam("output", output);
        return new ChainingProcessBuilder(command, error, output);
    }

    public ProcessStatusChecker getChecker() {
        return checker;
    }

    /**
     * Set the process exit status checker.  By default, the checker throws an exception when the exit code is nonzero.
     *
     * @param checker the exit status checker (must not be {@code null})
     */
    public void setChecker(final ProcessStatusChecker checker) {
        Assert.checkNotNullParam("checker", checker);
        this.checker = checker;
    }

    public ChainingProcessBuilder addArgs(String... args) {
        Assert.checkNotNullParam("args", args);
        Collections.addAll(command, args);
        return this;
    }

    public ChainingProcessBuilder addArgs(Collection<String> args) {
        Assert.checkNotNullParam("args", args);
        command.addAll(args);
        return this;
    }

    public ChainingProcessBuilder addArgs(String args) {
        Assert.checkNotNullParam("args", args);
        command.add(args);
        return this;
    }

    void handleOutput(final ProcessBuilder process) {
        // cannot be the last process in the chain
        throw Assert.unreachableCode();
    }

    void chain(List<Process> processList, int index) throws Exception {
        Process process = processList.get(index);
        // wait for process
        String name = "Waiter thread for process " + nameOf(process);
        try (Closeable c1 = new ThreadCloseable(new CloseableThread(name) {
            void runWithException() throws IOException {
                for (;;) {
                    try {
                        process.waitFor();
                        checker.checkProcessStatus(process);
                        return;
                    } catch (InterruptedException e) {
                        process.destroy();
                    }
                }
            }
        })) {
            // wait for error
            try (Closeable c2 = error.getErrorHandler(process)) {
                if (output instanceof ChainingProcessBuilder) {
                    ((ChainingProcessBuilder) output).chain(processList, index + 1);
                } else {
                    assert index == processList.size() - 1;
                    // wait for output
                    output.getOutputHandler(process).close();
                }
            }
        }
    }

    static final Closeable BLANK_CLOSEABLE = new Closeable() {
        public void close() {
        }
    };

    static String nameOf(final Process p) {
        return p.info().command().orElse("<unknown process>");
    }

    List<Process> start(ProcessInputProvider inputProvider) throws IOException {
        final List<ProcessBuilder> list = makeList(null, 0);
        assert list.size() >= 1;
        inputProvider.provideInput(list.get(0));
        return ProcessBuilder.startPipeline(list);
    }

    List<ProcessBuilder> makeList(final ProcessBuilder previous, final int index) {
        final ProcessBuilder processBuilder = this.processBuilder;
        processBuilder.command(command);
        final List<ProcessBuilder> list = output.makeList(processBuilder, index + 1);
        list.set(index, processBuilder);
        return list;
    }

    public void execute(ProcessInputProvider inputProvider) throws IOException {
        Assert.checkNotNullParam("inputProvider", inputProvider);
        final ProcessStatusChecker checker = this.checker;
        final List<Process> list;
        try {
            list = start(inputProvider);
        } catch (IOException e) {
            throw e;
        }
        try {
            chain(list, 0);
        } catch (IOException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IOException(t);
        }
    }
}

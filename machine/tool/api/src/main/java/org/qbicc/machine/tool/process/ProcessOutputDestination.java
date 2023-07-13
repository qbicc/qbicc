package org.qbicc.machine.tool.process;

import static org.qbicc.machine.tool.process.InputSource.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;

import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.ExceptionFunction;
import io.smallrye.common.function.ExceptionUnaryOperator;

final class ProcessOutputDestination<T> extends OutputDestination {
    static final Function<Process, InputSource> DEFAULT_CHAIN_SOURCE = process -> new InputStreamSupplierInputSource<>(ExceptionUnaryOperator.identity(), process.getInputStream());
    static final ExceptionConsumer<Process, IOException> DEFAULT_CHECKER = p -> {
        if (p.exitValue() != 0) {
            throw new IOException("Process returned exit code " + p.exitValue());
        }
    };

    private final OutputDestination output;
    private final OutputDestination error;
    private final Function<Process, InputSource> chainSource = DEFAULT_CHAIN_SOURCE;
    private final ExceptionConsumer<Process, IOException> checker;
    private final boolean breakChain = false;
    private final ExceptionFunction<T, ProcessBuilder, IOException> supplier;
    private final T param;

    ProcessOutputDestination(final ExceptionFunction<T, ProcessBuilder, IOException> supplier, final T param, final OutputDestination errorDest, final OutputDestination outputDest, final ExceptionConsumer<Process, IOException> checker) {
        this.supplier = supplier;
        this.param = param;
        output = outputDest;
        error = errorDest;
        this.checker = checker;
    }

    List<ProcessBuilder> getBuilderPipeline(final ProcessBuilder.Redirect inputRedirect, int index) throws IOException {
        if (breakChain && index != 0) {
            // break the chain (for example if process output was written to an intermediate file)
            return super.getBuilderPipeline(inputRedirect, index);
        }
        ProcessBuilder pb = supplier.apply(param);
        pb.redirectInput(inputRedirect);
        // stderr uses VM pipes
        pb.redirectError(error.getOutputRedirect());
        // stdout uses pipelining, if any
        pb.redirectOutput(output.getOutputRedirect());
        List<ProcessBuilder> pipeline = output.getBuilderPipeline(ProcessBuilder.Redirect.PIPE, index + 1);
        pipeline.set(index, pb);
        return pipeline;
    }

    void chain(List<Process> processes, final List<ProcessBuilder> processBuilders, int index) throws IOException {
        if (index == processes.size()) {
            // a process will only be at the end of the chain if the chain is deliberately broken
            super.chain(processes, processBuilders, index);
            return;
        }
        Process process = processes.get(index);
        // wait for process
        String name = "Waiter thread for process " + nameOf(process);
        try (Closeable c1 = Closeables.start(name, () -> {
            for (;;) {
                try {
                    process.waitFor();
                    return;
                } catch (InterruptedException e) {
                    process.destroy();
                }
            }
        })) {
            // ensure the error stream gets closed
            try (InputStream errorStream = process.getErrorStream()) {
                // wait for error
                try (Closeable c2 = error.transferFromErrorOf(process, processBuilders.get(index).redirectError())) {
                    // wait for output
                    output.chain(processes, processBuilders, index + 1);
                }
            }
        }
        checker.accept(process);
    }

    void transferFrom(final InputSource source) throws IOException {
        ProcessBuilder.Redirect inputRedirect = source.getInputRedirect();
        List<ProcessBuilder> processBuilders = getBuilderPipeline(inputRedirect, 0);
        List<Process> processes = ProcessBuilder.startPipeline(processBuilders);
        try (Closeable c = source.provideProcessInput(processes.get(0), inputRedirect)) {
            chain(processes, processBuilders, 0);
        }
    }

    void transferFrom(final InputStream stream) throws IOException {
        // start a nested pipeline using the input stream
        transferFrom(new InputStreamSupplierInputSource<InputStream>(ExceptionUnaryOperator.identity(), stream));
    }

    void transferFrom(final Reader reader, final Charset charset) throws IOException {
        // start a nested pipeline using the reader
        transferFrom(new ReaderSupplierInputSource<Reader>(ExceptionUnaryOperator.identity(), reader, charset));
    }

    ProcessBuilder.Redirect getOutputRedirect() {
        return ProcessBuilder.Redirect.PIPE;
    }
}

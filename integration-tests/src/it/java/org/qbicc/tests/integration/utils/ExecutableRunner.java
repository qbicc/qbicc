package org.qbicc.tests.integration.utils;

import org.jboss.logging.Logger;
import org.qbicc.machine.arch.Cpu;
import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.machine.tool.process.OutputDestination;

import java.io.IOException;
import java.nio.file.Path;

public interface ExecutableRunner {
    void run(String name, Path outputExecutable, StringBuilder stdOut, StringBuilder stdErr, Logger logger) throws IOException;

    static ExecutableRunner forPlatform(Platform plat) {
        if (plat.getCpu() == Cpu.WASM32) {
            return Wasm;
        } else {
            return Native;
        }
    }

    ExecutableRunner Native = (name, outputExecutable, stdOut, stdErr, logger) -> {
        makeRunner(stdOut, stdErr, new ProcessBuilder(outputExecutable.toString(), name), logger, name);
    };

    ExecutableRunner Wasm = (name, outputExecutable, stdOut, stdErr, logger) -> {
        makeRunner(stdOut, stdErr, new ProcessBuilder("node", outputExecutable.toString(), name), logger, name);
    };

    private static void makeRunner(StringBuilder stdOut, StringBuilder stdErr, ProcessBuilder processBuilder, Logger logger, String name) throws IOException {
        OutputDestination stdOutDest = OutputDestination.of(stdOut);
        OutputDestination stdErrDest = OutputDestination.of(stdErr);
        OutputDestination process = OutputDestination.of(processBuilder, stdErrDest, stdOutDest);
        InputSource.empty().transferTo(process);

        logger.infof("Process(" + name + ") standard output:%n%s", stdOut);
        if (!stdErr.toString().isBlank()) {
            logger.warnf("Process(" + name + ") standard error:%n%s", stdErr);
        }
    }


}

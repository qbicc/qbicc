package org.qbicc.tests.integration.utils;

import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.machine.tool.process.OutputDestination;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Path;

public class NativeExecutable {
    public static void run(String name, Path outputExecutable, StringBuilder stdOut, StringBuilder stdErr, Logger logger) throws IOException {
        OutputDestination stdOutDest = OutputDestination.of(stdOut);
        OutputDestination stdErrDest = OutputDestination.of(stdErr);
        ProcessBuilder processBuilder = new ProcessBuilder(outputExecutable.toString());
        OutputDestination process = OutputDestination.of(processBuilder, stdErrDest, stdOutDest);
        InputSource.empty().transferTo(process);

        logger.infof("Process(" + name + ") standard output:%n%s", stdOut);
        if (!stdErr.toString().isBlank()) {
            logger.warnf("Process(" + name + ") standard error:%n%s", stdErr);
        }
    }


}

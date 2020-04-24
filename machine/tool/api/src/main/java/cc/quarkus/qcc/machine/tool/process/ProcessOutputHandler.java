package cc.quarkus.qcc.machine.tool.process;

import java.io.Closeable;
import java.io.IOException;

/**
 *
 */
public abstract class ProcessOutputHandler extends ProcessChainTarget {
    ProcessOutputHandler() {}

    abstract void handleError(ProcessBuilder process);

    Closeable getErrorHandler(Process process) throws IOException {
        return ChainingProcessBuilder.BLANK_CLOSEABLE;
    }
}

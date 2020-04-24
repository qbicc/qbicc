package cc.quarkus.qcc.machine.tool.process;

import java.io.Closeable;
import java.io.IOException;

/**
 *
 */
public abstract class ProcessInputProvider {
    ProcessInputProvider() {}

    abstract void provideInput(final ProcessBuilder firstProcess);

    Closeable getInputHandler(Process process) throws IOException {
        return ChainingProcessBuilder.BLANK_CLOSEABLE;
    }
}

package cc.quarkus.qcc.machine.tool.process;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * A destination for process data.
 */
public abstract class ProcessChainTarget {
    ProcessChainTarget() {}

    List<ProcessBuilder> makeList(final ProcessBuilder previous, int index) {
        final List<ProcessBuilder> list = Arrays.asList(new ProcessBuilder[index]);
        final ProcessBuilder last = list.get(list.size() - 1);
        handleOutput(last);
        return list;
    }

    abstract void handleOutput(ProcessBuilder process);

    Closeable getOutputHandler(Process process) throws IOException {
        return ChainingProcessBuilder.BLANK_CLOSEABLE;
    };
}

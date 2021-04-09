package org.qbicc.machine.tool;

import java.io.IOException;
import java.nio.file.Path;

/**
 * An invoker for an external program such as a compiler or linker.
 */
public interface ToolInvoker {
    /**
     * Get the tool corresponding to the invoker.
     *
     * @return the tool (not {@code null})
     */
    Tool getTool();

    /**
     * Get the path of the program that was run.
     *
     * @return the path (not {@code null})
     */
    Path getPath();

    /**
     * Invoke the program.
     *
     * @throws IOException if program invocation failed for some reason
     */
    void invoke() throws IOException;
}

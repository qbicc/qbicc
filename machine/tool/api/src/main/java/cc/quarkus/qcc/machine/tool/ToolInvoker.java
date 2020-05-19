package cc.quarkus.qcc.machine.tool;

import java.io.IOException;

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
     * Invoke the program.
     *
     * @throws IOException if program invocation failed for some reason
     */
    void invoke() throws IOException;
}

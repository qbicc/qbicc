package cc.quarkus.qcc.machine.tool;

import cc.quarkus.qcc.machine.tool.process.InputSource;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;

/**
 *
 */
public abstract class InvokerBuilder {
    private final Tool tool;

    protected InvokerBuilder(final Tool tool) {
        this.tool = tool;
    }

    public Tool getTool() {
        return tool;
    }

    /**
     * Construct an invoker that will compile a program provided to the returned destination.  The result
     * of the compilation will be passed to the configured message handler and output destination.
     *
     * @return the compiler invoker
     * @see InputSource#transferTo(OutputDestination)
     */
    public abstract OutputDestination build();
}

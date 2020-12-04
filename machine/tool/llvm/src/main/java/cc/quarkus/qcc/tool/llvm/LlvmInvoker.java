package cc.quarkus.qcc.tool.llvm;

import cc.quarkus.qcc.machine.tool.MessagingToolInvoker;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;

/**
 *
 */
public interface LlvmInvoker extends MessagingToolInvoker {
    LlvmToolChain getTool();

    void setSource(InputSource source);

    InputSource getSource();

    void setDestination(OutputDestination destination);

    OutputDestination getDestination();

    /**
     * Get an output destination that invokes this invoker.  The invoker should not be modified
     * before using the output destination.
     *
     * @return the output destination
     */
    OutputDestination invokerAsDestination();
}

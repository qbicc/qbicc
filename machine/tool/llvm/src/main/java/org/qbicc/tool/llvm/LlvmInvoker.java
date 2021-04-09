package org.qbicc.tool.llvm;

import org.qbicc.machine.tool.MessagingToolInvoker;
import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.machine.tool.process.OutputDestination;

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

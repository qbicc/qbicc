package org.qbicc.machine.tool;

/**
 * A tool invoker which invokes a tool that produces messages.
 */
public interface MessagingToolInvoker extends ToolInvoker {
    /**
     * Set the message handler for this invoker.
     *
     * @param messageHandler the message handler to use (must not be {@code null})
     */
    void setMessageHandler(ToolMessageHandler messageHandler);

    /**
     * Get the currently set message handler for this invoker.
     *
     * @return the message handler (not {@code null})
     */
    ToolMessageHandler getMessageHandler();
}

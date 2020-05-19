package cc.quarkus.qcc.machine.tool;

import java.io.IOException;

/**
 *
 */
public class ToolExecutionFailureException extends IOException {
    private static final long serialVersionUID = -219221983613643801L;

    /**
     * Constructs a new {@code ToolExecutionFailureException} instance. The message is left blank ({@code null}), and
     * no cause is specified.
     */
    public ToolExecutionFailureException() {
    }

    /**
     * Constructs a new {@code ToolExecutionFailureException} instance with an initial message. No cause is specified.
     *
     * @param msg the message
     */
    public ToolExecutionFailureException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code ToolExecutionFailureException} instance with an initial cause. If a non-{@code null}
     * cause is specified, its message is used to initialize the message of this {@code ToolExecutionFailureException};
     * otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public ToolExecutionFailureException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code ToolExecutionFailureException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public ToolExecutionFailureException(final String msg, final Throwable cause) {
        super(msg + ": " + cause, cause);
    }
}

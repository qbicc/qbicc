package org.qbicc.machine.tool;

/**
 * An exception thrown when a compilation process executes but exits with a non-zero status.
 */
public class CompilationFailureException extends ToolExecutionFailureException {
    private static final long serialVersionUID = 4471599851611398438L;

    /**
     * Constructs a new {@code CompilationFailureException} instance.  The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public CompilationFailureException() {
    }

    /**
     * Constructs a new {@code CompilationFailureException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public CompilationFailureException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code CompilationFailureException} instance with an initial cause.  If a non-{@code null} cause
     * is specified, its message is used to initialize the message of this {@code CompilationFailureException};
     * otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public CompilationFailureException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code CompilationFailureException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public CompilationFailureException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

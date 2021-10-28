package org.qbicc.interpreter;

import java.io.Serial;

/**
 * An exception thrown when the interpreter is halted.
 */
public class InterpreterHaltedException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -8299653786740116372L;

    /**
     * Constructs a new {@code InterpreterHaltedException} instance.  The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public InterpreterHaltedException() {
    }

    /**
     * Constructs a new {@code InterpreterHaltedException} instance with an initial message.  No
     * cause is specified.
     *
     * @param msg the message
     */
    public InterpreterHaltedException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code InterpreterHaltedException} instance with an initial cause.  If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code InterpreterHaltedException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public InterpreterHaltedException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code InterpreterHaltedException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public InterpreterHaltedException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

}

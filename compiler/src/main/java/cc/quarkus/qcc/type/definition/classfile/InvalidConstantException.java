package org.qbicc.type.definition.classfile;

/**
 *
 */
public class InvalidConstantException extends ClassVerificationFailureException {
    private static final long serialVersionUID = 8186847697905146548L;

    /**
     * Constructs a new {@code InvalidConstantException} instance.  The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public InvalidConstantException() {
    }

    /**
     * Constructs a new {@code InvalidConstantException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public InvalidConstantException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code InvalidConstantException} instance with an initial cause.  If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code InvalidConstantException}; otherwise the
     * message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public InvalidConstantException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code InvalidConstantException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public InvalidConstantException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

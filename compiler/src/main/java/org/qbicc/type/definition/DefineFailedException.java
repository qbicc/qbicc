package org.qbicc.type.definition;

/**
 *
 */
public class DefineFailedException extends org.qbicc.type.definition.LinkageException {
    private static final long serialVersionUID = - 8151194445449279950L;

    /**
     * Constructs a new {@code DefineFailedException} instance.  The message is left blank ({@code null}), and no cause
     * is specified.
     */
    public DefineFailedException() {
    }

    /**
     * Constructs a new {@code DefineFailedException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public DefineFailedException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code DefineFailedException} instance with an initial cause.  If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code DefineFailedException}; otherwise the
     * message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public DefineFailedException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code DefineFailedException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public DefineFailedException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

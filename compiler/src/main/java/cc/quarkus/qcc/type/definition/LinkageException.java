package org.qbicc.type.definition;

/**
 *
 */
public class LinkageException extends RuntimeException {
    private static final long serialVersionUID = - 1666470124185645972L;

    /**
     * Constructs a new {@code LinkageException} instance.  The message is left blank ({@code null}), and no cause is
     * specified.
     */
    public LinkageException() {
    }

    /**
     * Constructs a new {@code LinkageException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public LinkageException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code LinkageException} instance with an initial cause.  If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code LinkageException}; otherwise the message
     * is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public LinkageException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code LinkageException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public LinkageException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

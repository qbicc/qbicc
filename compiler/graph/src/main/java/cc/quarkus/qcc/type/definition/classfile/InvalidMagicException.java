package cc.quarkus.qcc.type.definition.classfile;

/**
 *
 */
public class InvalidMagicException extends ClassFormatException {
    private static final long serialVersionUID = 88033322855643945L;

    /**
     * Constructs a new {@code InvalidMagicException} instance.  The message is left blank ({@code null}), and no cause
     * is specified.
     */
    public InvalidMagicException() {
    }

    /**
     * Constructs a new {@code InvalidMagicException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public InvalidMagicException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code InvalidMagicException} instance with an initial cause.  If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code InvalidMagicException}; otherwise the
     * message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public InvalidMagicException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code InvalidMagicException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public InvalidMagicException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

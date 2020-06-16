package cc.quarkus.qcc.type.definition;

/**
 *
 */
public class VerifyFailedException extends LinkageException {
    private static final long serialVersionUID = 2999615213299022852L;

    /**
     * Constructs a new {@code VerifyFailedException} instance.  The message is left blank ({@code null}), and no cause
     * is specified.
     */
    public VerifyFailedException() {
    }

    /**
     * Constructs a new {@code VerifyFailedException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public VerifyFailedException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code VerifyFailedException} instance with an initial cause.  If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code VerifyFailedException}; otherwise the
     * message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public VerifyFailedException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code VerifyFailedException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public VerifyFailedException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

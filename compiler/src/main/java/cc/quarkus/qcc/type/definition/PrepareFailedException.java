package cc.quarkus.qcc.type.definition;

/**
 *
 */
public class PrepareFailedException extends LinkageException {
    private static final long serialVersionUID = 3400499795545894935L;

    /**
     * Constructs a new {@code PrepareFailedException} instance.  The message is left blank ({@code null}), and no cause
     * is specified.
     */
    public PrepareFailedException() {
    }

    /**
     * Constructs a new {@code PrepareFailedException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public PrepareFailedException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code PrepareFailedException} instance with an initial cause.  If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code PrepareFailedException}; otherwise the
     * message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public PrepareFailedException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code PrepareFailedException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public PrepareFailedException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

package cc.quarkus.qcc.type.definition;

/**
 *
 */
public class ResolutionFailedException extends LinkageException {
    private static final long serialVersionUID = 1230090955216502334L;

    /**
     * Constructs a new {@code ResolutionFailedException} instance.  The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public ResolutionFailedException() {
    }

    /**
     * Constructs a new {@code ResolutionFailedException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public ResolutionFailedException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code ResolutionFailedException} instance with an initial cause.  If a non-{@code null} cause
     * is specified, its message is used to initialize the message of this {@code ResolutionFailedException}; otherwise
     * the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public ResolutionFailedException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code ResolutionFailedException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public ResolutionFailedException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

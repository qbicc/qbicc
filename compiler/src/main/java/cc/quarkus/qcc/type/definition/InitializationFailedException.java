package cc.quarkus.qcc.type.definition;

/**
 *
 */
public class InitializationFailedException extends LinkageException {
    private static final long serialVersionUID = - 8103250836471487429L;

    /**
     * Constructs a new {@code InitializationFailedException} instance.  The message is left blank ({@code null}), and
     * no cause is specified.
     */
    public InitializationFailedException() {
    }

    /**
     * Constructs a new {@code InitializationFailedException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public InitializationFailedException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code InitializationFailedException} instance with an initial cause.  If a non-{@code null}
     * cause is specified, its message is used to initialize the message of this {@code InitializationFailedException};
     * otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public InitializationFailedException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code InitializationFailedException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public InitializationFailedException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

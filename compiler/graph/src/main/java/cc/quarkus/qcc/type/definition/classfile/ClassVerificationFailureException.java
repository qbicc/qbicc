package cc.quarkus.qcc.type.definition.classfile;

/**
 *
 */
public abstract class ClassVerificationFailureException extends ClassFormatException {
    private static final long serialVersionUID = - 6402387074739664844L;

    /**
     * Constructs a new {@code ClassVerificationFailureException} instance.  The message is left blank ({@code null}),
     * and no cause is specified.
     */
    public ClassVerificationFailureException() {
    }

    /**
     * Constructs a new {@code ClassVerificationFailureException} instance with an initial message.  No cause is
     * specified.
     *
     * @param msg the message
     */
    public ClassVerificationFailureException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code ClassVerificationFailureException} instance with an initial cause.  If a non-{@code null}
     * cause is specified, its message is used to initialize the message of this {@code
     * ClassVerificationFailureException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public ClassVerificationFailureException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code ClassVerificationFailureException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public ClassVerificationFailureException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

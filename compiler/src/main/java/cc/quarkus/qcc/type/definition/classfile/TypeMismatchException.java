package cc.quarkus.qcc.type.definition.classfile;

/**
 *
 */
public class TypeMismatchException extends ClassVerificationFailureException {
    private static final long serialVersionUID = - 8696205774007062401L;

    /**
     * Constructs a new {@code TypeMismatchException} instance.  The message is left blank ({@code null}), and no cause
     * is specified.
     */
    public TypeMismatchException() {
    }

    /**
     * Constructs a new {@code TypeMismatchException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public TypeMismatchException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code TypeMismatchException} instance with an initial cause.  If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code TypeMismatchException}; otherwise the
     * message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public TypeMismatchException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code TypeMismatchException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public TypeMismatchException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

package cc.quarkus.qcc.type.definition.classfile;

/**
 *
 */
public class MissingStackMapTableException extends ClassVerificationFailureException {
    private static final long serialVersionUID = - 4490454684230968594L;

    /**
     * Constructs a new {@code MissingStackMapTableException} instance.  The message is left blank ({@code null}), and
     * no cause is specified.
     */
    public MissingStackMapTableException() {
    }

    /**
     * Constructs a new {@code MissingStackMapTableException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public MissingStackMapTableException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code MissingStackMapTableException} instance with an initial cause.  If a non-{@code null}
     * cause is specified, its message is used to initialize the message of this {@code MissingStackMapTableException};
     * otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public MissingStackMapTableException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code MissingStackMapTableException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public MissingStackMapTableException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

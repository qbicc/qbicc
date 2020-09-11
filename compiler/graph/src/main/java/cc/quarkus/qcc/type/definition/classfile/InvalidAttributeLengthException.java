package cc.quarkus.qcc.type.definition.classfile;

/**
 *
 */
public class InvalidAttributeLengthException extends ClassFormatException {
    private static final long serialVersionUID = - 1044915153218816317L;

    /**
     * Constructs a new {@code InvalidAttributeLengthException} instance.  The message is left blank ({@code null}), and
     * no cause is specified.
     */
    public InvalidAttributeLengthException() {
    }

    /**
     * Constructs a new {@code InvalidAttributeLengthException} instance with an initial message.  No cause is
     * specified.
     *
     * @param msg the message
     */
    public InvalidAttributeLengthException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code InvalidAttributeLengthException} instance with an initial cause.  If a non-{@code null}
     * cause is specified, its message is used to initialize the message of this {@code
     * InvalidAttributeLengthException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public InvalidAttributeLengthException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code InvalidAttributeLengthException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public InvalidAttributeLengthException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

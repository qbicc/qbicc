package cc.quarkus.qcc.type.definition.classfile;

/**
 *
 */
public class InvalidAnnotationValueException extends ClassFormatException {
    private static final long serialVersionUID = 792490552014220976L;

    /**
     * Constructs a new {@code InvalidAnnotationValueException} instance.  The message is left blank ({@code null}), and
     * no cause is specified.
     */
    public InvalidAnnotationValueException() {
    }

    /**
     * Constructs a new {@code InvalidAnnotationValueException} instance with an initial message.  No cause is
     * specified.
     *
     * @param msg the message
     */
    public InvalidAnnotationValueException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code InvalidAnnotationValueException} instance with an initial cause.  If a non-{@code null}
     * cause is specified, its message is used to initialize the message of this {@code
     * InvalidAnnotationValueException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public InvalidAnnotationValueException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code InvalidAnnotationValueException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public InvalidAnnotationValueException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

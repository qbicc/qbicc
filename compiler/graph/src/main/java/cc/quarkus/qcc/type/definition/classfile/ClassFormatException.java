package cc.quarkus.qcc.type.definition.classfile;

/**
 * A general class format related exception.
 */
public abstract class ClassFormatException extends IllegalArgumentException {
    private static final long serialVersionUID = 772300632866950459L;

    /**
     * Constructs a new {@code ClassFormatException} instance.  The message is left blank ({@code null}), and no cause
     * is specified.
     */
    public ClassFormatException() {
    }

    /**
     * Constructs a new {@code ClassFormatException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public ClassFormatException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code ClassFormatException} instance with an initial cause.  If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code ClassFormatException}; otherwise the
     * message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public ClassFormatException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code ClassFormatException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public ClassFormatException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

package org.qbicc.type.definition.classfile;

/**
 * Exception thrown when a class file constant type does not match what was expected.
 */
public class ConstantTypeMismatchException extends IllegalArgumentException {
    private static final long serialVersionUID = - 7574243455495130414L;

    /**
     * Constructs a new {@code ConstantTypeMismatchException} instance.  The message is left blank ({@code null}), and
     * no cause is specified.
     */
    public ConstantTypeMismatchException() {
    }

    /**
     * Constructs a new {@code ConstantTypeMismatchException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public ConstantTypeMismatchException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code ConstantTypeMismatchException} instance with an initial cause.  If a non-{@code null}
     * cause is specified, its message is used to initialize the message of this {@code ConstantTypeMismatchException};
     * otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public ConstantTypeMismatchException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code ConstantTypeMismatchException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public ConstantTypeMismatchException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

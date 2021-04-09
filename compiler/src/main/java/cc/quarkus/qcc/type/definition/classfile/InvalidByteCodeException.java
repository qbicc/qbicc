package org.qbicc.type.definition.classfile;

/**
 *
 */
public class InvalidByteCodeException extends ClassFormatException {
    private static final long serialVersionUID = 7072605413861377655L;

    /**
     * Constructs a new {@code InvalidByteCodeException} instance.  The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public InvalidByteCodeException() {
    }

    /**
     * Constructs a new {@code InvalidByteCodeException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public InvalidByteCodeException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code InvalidByteCodeException} instance with an initial cause.  If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code InvalidByteCodeException}; otherwise the
     * message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public InvalidByteCodeException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code InvalidByteCodeException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public InvalidByteCodeException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

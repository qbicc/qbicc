package org.qbicc.type.definition.classfile;

/**
 *
 */
public class UnsupportedClassVersionException extends ClassFormatException {
    private static final long serialVersionUID = - 3595611835616991714L;

    /**
     * Constructs a new {@code UnsupportedClassVersionException} instance.  The message is left blank ({@code null}),
     * and no cause is specified.
     */
    public UnsupportedClassVersionException() {
    }

    /**
     * Constructs a new {@code UnsupportedClassVersionException} instance with an initial message.  No cause is
     * specified.
     *
     * @param msg the message
     */
    public UnsupportedClassVersionException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code UnsupportedClassVersionException} instance with an initial cause.  If a non-{@code null}
     * cause is specified, its message is used to initialize the message of this {@code
     * UnsupportedClassVersionException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public UnsupportedClassVersionException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code UnsupportedClassVersionException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public UnsupportedClassVersionException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

package org.qbicc.type.definition.classfile;

/**
 *
 */
public class InvalidTypeDescriptorException extends ClassFormatException {
    private static final long serialVersionUID = 7527898993103055982L;

    /**
     * Constructs a new {@code InvalidTypeDescriptorException} instance.  The message is left blank ({@code null}), and
     * no cause is specified.
     */
    public InvalidTypeDescriptorException() {
    }

    /**
     * Constructs a new {@code InvalidTypeDescriptorException} instance with an initial message.  No cause is
     * specified.
     *
     * @param msg the message
     */
    public InvalidTypeDescriptorException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code InvalidTypeDescriptorException} instance with an initial cause.  If a non-{@code null}
     * cause is specified, its message is used to initialize the message of this {@code InvalidTypeDescriptorException};
     * otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public InvalidTypeDescriptorException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code InvalidTypeDescriptorException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public InvalidTypeDescriptorException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

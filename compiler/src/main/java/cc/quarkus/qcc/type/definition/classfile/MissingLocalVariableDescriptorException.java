package org.qbicc.type.definition.classfile;

/**
 *
 */
public class MissingLocalVariableDescriptorException extends ClassVerificationFailureException {
    private static final long serialVersionUID = - 5478395133937795111L;

    /**
     * Constructs a new {@code MissingLocalVariableDescriptorException} instance.  The message is left blank ({@code
     * null}), and no cause is specified.
     */
    public MissingLocalVariableDescriptorException() {
    }

    /**
     * Constructs a new {@code MissingLocalVariableDescriptorException} instance with an initial message.  No cause is
     * specified.
     *
     * @param msg the message
     */
    public MissingLocalVariableDescriptorException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code MissingLocalVariableDescriptorException} instance with an initial cause.  If a non-{@code
     * null} cause is specified, its message is used to initialize the message of this {@code
     * MissingLocalVariableDescriptorException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public MissingLocalVariableDescriptorException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code MissingLocalVariableDescriptorException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public MissingLocalVariableDescriptorException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

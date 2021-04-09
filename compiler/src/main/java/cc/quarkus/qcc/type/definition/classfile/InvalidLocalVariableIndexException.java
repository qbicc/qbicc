package org.qbicc.type.definition.classfile;

/**
 *
 */
public class InvalidLocalVariableIndexException extends ClassFormatException {
    private static final long serialVersionUID = 8692699503154135621L;

    /**
     * Constructs a new {@code InvalidLocalVariableIndexException} instance.  The message is left blank ({@code null}),
     * and no cause is specified.
     */
    public InvalidLocalVariableIndexException() {
    }

    /**
     * Constructs a new {@code InvalidLocalVariableIndexException} instance with an initial message.  No cause is
     * specified.
     *
     * @param msg the message
     */
    public InvalidLocalVariableIndexException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code InvalidLocalVariableIndexException} instance with an initial cause.  If a non-{@code
     * null} cause is specified, its message is used to initialize the message of this {@code
     * InvalidLocalVariableIndexException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public InvalidLocalVariableIndexException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code InvalidLocalVariableIndexException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public InvalidLocalVariableIndexException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

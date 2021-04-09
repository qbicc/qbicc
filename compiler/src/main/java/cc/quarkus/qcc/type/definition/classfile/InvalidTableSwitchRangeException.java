package org.qbicc.type.definition.classfile;

/**
 *
 */
public class InvalidTableSwitchRangeException extends ClassFormatException {
    private static final long serialVersionUID = 3766700203292332873L;

    /**
     * Constructs a new {@code InvalidTableSwitchRangeException} instance.  The message is left blank ({@code null}),
     * and no cause is specified.
     */
    public InvalidTableSwitchRangeException() {
    }

    /**
     * Constructs a new {@code InvalidTableSwitchRangeException} instance with an initial message.  No cause is
     * specified.
     *
     * @param msg the message
     */
    public InvalidTableSwitchRangeException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code InvalidTableSwitchRangeException} instance with an initial cause.  If a non-{@code null}
     * cause is specified, its message is used to initialize the message of this {@code
     * InvalidTableSwitchRangeException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public InvalidTableSwitchRangeException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code InvalidTableSwitchRangeException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public InvalidTableSwitchRangeException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

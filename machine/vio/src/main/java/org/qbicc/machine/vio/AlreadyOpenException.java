package org.qbicc.machine.vio;

import java.io.IOException;
import java.io.Serial;

/**
 * An exception thrown when an open operation is performed on something that is already open.
 */
public class AlreadyOpenException extends IOException {
    @Serial
    private static final long serialVersionUID = 8272177789215012690L;

    /**
     * Constructs a new {@code AlreadyOpenException} instance.  The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public AlreadyOpenException() {
    }

    /**
     * Constructs a new {@code AlreadyOpenException} instance with an initial message.  No
     * cause is specified.
     *
     * @param msg the message
     */
    public AlreadyOpenException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code AlreadyOpenException} instance with an initial cause.  If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code AlreadyOpenException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public AlreadyOpenException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code AlreadyOpenException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public AlreadyOpenException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

}

package org.qbicc.machine.vio;

import java.io.IOException;
import java.io.Serial;

/**
 * An exception thrown when an operation is attempted on a file descriptor that is not open or is out of range.
 */
public class BadDescriptorException extends IOException {
    @Serial
    private static final long serialVersionUID = -2102168637771909705L;

    /**
     * Constructs a new {@code BadDescriptorException} instance.  The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public BadDescriptorException() {
    }

    /**
     * Constructs a new {@code BadDescriptorException} instance with an initial message.  No
     * cause is specified.
     *
     * @param msg the message
     */
    public BadDescriptorException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code BadDescriptorException} instance with an initial cause.  If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code BadDescriptorException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public BadDescriptorException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code BadDescriptorException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public BadDescriptorException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    static BadDescriptorException fileNotOpen() {
        return new BadDescriptorException("File not open");
    }
}

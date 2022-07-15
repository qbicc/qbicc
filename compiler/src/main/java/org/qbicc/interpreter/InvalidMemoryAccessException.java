package org.qbicc.interpreter;

import java.io.Serial;

/**
 * An exception indicating that a memory access would violate the strong type of the memory.
 */
public class InvalidMemoryAccessException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -615690580354323808L;

    /**
     * Constructs a new {@code InvalidMemoryAccessException} instance.  The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public InvalidMemoryAccessException() {
    }

    /**
     * Constructs a new {@code InvalidMemoryAccessException} instance with an initial message.  No
     * cause is specified.
     *
     * @param msg the message
     */
    public InvalidMemoryAccessException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code InvalidMemoryAccessException} instance with an initial cause.  If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code InvalidMemoryAccessException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public InvalidMemoryAccessException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code InvalidMemoryAccessException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public InvalidMemoryAccessException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

}

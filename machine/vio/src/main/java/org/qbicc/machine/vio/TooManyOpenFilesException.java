package org.qbicc.machine.vio;

import java.io.IOException;
import java.io.Serial;

/**
 * An exception which is thrown when there are too many open files.
 */
public class TooManyOpenFilesException extends IOException {
    @Serial
    private static final long serialVersionUID = 4050366742152256613L;

    /**
     * Constructs a new {@code TooManyOpenFilesException} instance.  The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public TooManyOpenFilesException() {
    }

    /**
     * Constructs a new {@code TooManyOpenFilesException} instance with an initial message.  No
     * cause is specified.
     *
     * @param msg the message
     */
    public TooManyOpenFilesException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code TooManyOpenFilesException} instance with an initial cause.  If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code TooManyOpenFilesException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public TooManyOpenFilesException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code TooManyOpenFilesException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public TooManyOpenFilesException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

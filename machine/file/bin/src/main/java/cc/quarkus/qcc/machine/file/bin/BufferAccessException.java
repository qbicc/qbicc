package cc.quarkus.qcc.machine.file.bin;

/**
 * An exception thrown when a buffer access failed unexpectedly.
 */
public class BufferAccessException extends IllegalStateException {
    private static final long serialVersionUID = -3618514198192435486L;

    /**
     * Constructs a new {@code BufferAccessException} instance. The message is left blank ({@code null}), and no cause
     * is specified.
     */
    public BufferAccessException() {
    }

    /**
     * Constructs a new {@code BufferAccessException} instance with an initial message. No cause is specified.
     *
     * @param msg the message
     */
    public BufferAccessException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code BufferAccessException} instance with an initial cause. If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code BufferAccessException}; otherwise the
     * message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public BufferAccessException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code BufferAccessException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public BufferAccessException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

package org.qbicc.type.definition.classfile;

/**
 *
 */
public class InvalidStackMapFrameEntry extends ClassFormatException {
    private static final long serialVersionUID = 3200926465809672634L;

    /**
     * Constructs a new {@code InvalidStackMapFrameEntry} instance.  The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public InvalidStackMapFrameEntry() {
    }

    /**
     * Constructs a new {@code InvalidStackMapFrameEntry} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public InvalidStackMapFrameEntry(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code InvalidStackMapFrameEntry} instance with an initial cause.  If a non-{@code null} cause
     * is specified, its message is used to initialize the message of this {@code InvalidStackMapFrameEntry}; otherwise
     * the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public InvalidStackMapFrameEntry(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code InvalidStackMapFrameEntry} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public InvalidStackMapFrameEntry(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

package org.qbicc.machine.tool.process;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

final class AppendableWriter extends Writer {
    private final Appendable appendable;

    AppendableWriter(final Appendable appendable) {
        this.appendable = appendable;
    }

    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        if (appendable instanceof StringBuilder) {
            ((StringBuilder) appendable).append(cbuf, off, len);
        } else if (appendable instanceof CharBuffer) {
            ((CharBuffer) appendable).put(cbuf, off, len);
        } else {
            appendable.append(new String(cbuf, off, len));
        }
    }

    public void write(final int c) throws IOException {
        appendable.append((char) c);
    }

    public void write(final String str) throws IOException {
        appendable.append(str);
    }

    public void write(final String str, final int off, final int len) throws IOException {
        appendable.append(str, off, len);
    }

    public Writer append(final CharSequence csq) throws IOException {
        appendable.append(csq);
        return this;
    }

    public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
        appendable.append(csq, start, end);
        return this;
    }

    public Writer append(final char c) throws IOException {
        appendable.append(c);
        return this;
    }

    public void flush() {
    }

    public void close() {
    }
}

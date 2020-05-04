package cc.quarkus.qcc.machine.tool.process;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;

class CharSequenceReader extends Reader {
    private final CharSequence charSequence;
    private int mark;
    private int pos;

    CharSequenceReader(final CharSequence charSequence) {
        this.charSequence = charSequence;
    }

    public int read(final char[] cbuf, final int off, final int len) {
        CharSequence charSequence = this.charSequence;
        int length = charSequence.length();
        if (pos == length) {
            return - 1;
        }
        int cnt = Math.min(len, length - pos);
        if (charSequence instanceof StringBuilder) {
            ((StringBuilder) charSequence).getChars(pos, pos + cnt, cbuf, off);
        } else {
            for (int i = 0; i < cnt; i++) {
                cbuf[i + off] = charSequence.charAt(pos++);
            }
        }
        return cnt;
    }

    public boolean markSupported() {
        return true;
    }

    public long skip(final long n) {
        int length = charSequence.length();
        int cnt = (int) Math.min(n, length - pos);
        pos += cnt;
        return cnt;
    }

    public boolean ready() {
        return true;
    }

    public void mark(final int readAheadLimit) {
        mark = pos;
    }

    public void reset() {
        pos = mark;
    }

    public int read() {
        final int pos = this.pos;
        if (pos == charSequence.length()) {
            return - 1;
        } else {
            this.pos = pos + 1;
            return charSequence.charAt(pos);
        }
    }

    public long transferTo(final Writer out) throws IOException {
        int length = charSequence.length();
        int pos = this.pos;
        out.append(charSequence, pos, length);
        this.pos = length;
        return length - pos;
    }

    public int read(final CharBuffer target) {
        int length = charSequence.length();
        int pos = this.pos;
        int cnt = Math.min(target.remaining(), length - pos);
        target.append(charSequence, pos, pos + cnt);
        return cnt;
    }

    public void close() {
        pos = charSequence.length();
    }
}

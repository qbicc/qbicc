package org.qbicc.machine.tool.process;

import java.io.IOException;

/**
 * A limited-size appendable.
 */
public final class LimitedAppendable implements Appendable {
    final Appendable delegate;
    final int limit;
    int size;

    public LimitedAppendable(Appendable delegate, int limit) {
        this.delegate = delegate;
        this.limit = limit;
    }

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        return append(csq, 0, csq.length());
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        if (start > end || start < 0) {
            throw new IllegalArgumentException();
        }
        int amt = end - start;
        if (amt > 0) {
            final int rem = limit - size;
            if (amt < rem) {
                size += amt;
                delegate.append(csq, start, end);
            } else if (rem > 0) {
                size += rem;
                delegate.append(csq, start, start + rem);
            }
        }
        return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
        if (size < limit) {
            delegate.append(c);
        }
        return this;
    }
}

package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

final class ByteArrayImpl extends AbstractValue {
    final byte[] contents;

    ByteArrayImpl(byte[] contents) {
        this.contents = contents;
    }

    private static char hex(int v) {
        v &= 0xf;
        if (v <= 0x09) {
            return (char) ('0' + v);
        } else {
            return (char) ('A' + v - 10);
        }
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('c').append('"');
        for (int i = 0; i < contents.length; i ++) {
            int ch = contents[i] & 0xff;
            if (32 <= ch && ch <= 126 && ch != '\\' && ch != '"') {
                target.append((char) ch);
            } else {
                target.append('\\').append(hex(ch >> 4)).append(hex(ch));
            }
        }
        target.append('"');
        return target;
    }
}

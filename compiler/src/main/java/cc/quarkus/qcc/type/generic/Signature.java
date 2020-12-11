package cc.quarkus.qcc.type.generic;

import java.nio.ByteBuffer;

/**
 * An object representation of the value of the JVM {@code Signature} attribute.
 */
public abstract class Signature {
    private final int hashCode;

    Signature(final int hashCode) {
        this.hashCode = hashCode;
    }

    public final boolean equals(final Object obj) {
        return obj instanceof Signature && equals((Signature) obj);
    }

    public boolean equals(final Signature other) {
        return this == other || other != null && hashCode == other.hashCode;
    }

    public final String toString() {
        return toString(new StringBuilder()).toString();
    }

    public abstract StringBuilder toString(StringBuilder target);

    public final int hashCode() {
        return hashCode;
    }

    static int next(ByteBuffer buf) {
        return buf.get() & 0xff;
    }

    static int peek(ByteBuffer buf) {
        return buf.get(buf.position()) & 0xff;
    }

    static void expect(ByteBuffer buf, int val) {
        if (next(buf) != val) {
            throw parseError();
        }
    }

    static int codePoint(ByteBuffer buf) {
        int a = next(buf);
        if (a < 0x80) {
            return a;
        } else if (a < 0xc0) {
            throw parseError();
        } else if (a < 0xe0) {
            int b = next(buf);
            if (b < 0x80 || 0xbf < b) {
                throw parseError();
            }
            return (a & 0b0001_1111) << 6 | (b & 0b0011_1111);
        } else if (a < 0xf0) {
            int b = next(buf);
            if (b < 0x80 || 0xbf < b) {
                throw parseError();
            }
            int c = next(buf);
            if (c < 0x80 || 0xbf < c) {
                throw parseError();
            }
            return (a & 0b0000_1111) << 12 | (b & 0b0011_1111) << 6 | c & 0b0011_1111;
        } else {
            throw parseError();
        }
    }

    static IllegalArgumentException parseError() {
        return new IllegalArgumentException("Invalid generic signature string");
    }
}

package org.qbicc.runtime.deserialization;

/**
 * A subset of java.nio.HeapByteBuffer functionality needed by qbicc.
 * We roll our own class because we need this to work very early in
 * the program's startup sequence and therefore cannot depend on JDK classes
 * (since their static fields are being initialized by this code).
 */
public final class DeserializationBuffer {
    // TODO: This should be set at compile-time based on the endianness of the target.
    //       However, we are going to get rid of all of this deserialization code in
    //       favor of writing a fully formed heap soon, so just hardwire to LE as an expedient.
    static final boolean LE = true;

    private final byte[] buf;
    private int current;

    DeserializationBuffer(byte[] buf) {
        this.buf = buf;
        this.current = 0;
    }

    // TODO: this method is intended to be a qbicc intrinsic and will assume platform-endianness of buf
    private static int read16(byte[] buf, int offset) {
        byte b0 = buf[offset];
        byte b1 = buf[offset + 1];
        int bits;
        if (LE) {
            bits = (b0 & 0xFF) | ((b1 & 0xFF) << 8);
        } else {
            bits = (b1 & 0xFF) | ((b0 & 0xFF) << 8);
        }
        return bits;
    }

    // TODO: this method is intended to be a qbicc intrinsic and will assume platform-endianness of buf
    private static int read32(byte[] buf, int offset) {
        byte b0 = buf[offset];
        byte b1 = buf[offset + 1];
        byte b2 = buf[offset + 2];
        byte b3 = buf[offset + 3];
        int bits;
        if (LE) {
            bits = (b0 & 0xFF) | ((b1 & 0xFF) << 8) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 24);
        } else {
            bits = (b3 & 0xFF) | ((b2 & 0xFF) << 8) | ((b1 & 0xFF) << 16) | ((b0 & 0xFF) << 24);
        }
        return bits;
    }

    // TODO: this method is intended to be a qbicc intrinsic and will assume platform-endianness of buf
    private static long read64(byte[] buf, int offset) {
        byte b0 = buf[offset];
        byte b1 = buf[offset + 1];
        byte b2 = buf[offset + 2];
        byte b3 = buf[offset + 3];
        byte b4 = buf[offset + 4];
        byte b5 = buf[offset + 5];
        byte b6 = buf[offset + 6];
        byte b7 = buf[offset + 7];
        long bits;
        if (LE) {
            bits = (b0 & 0xFFL) | ((b1 & 0xFFL) << 8) | ((b2 & 0xFFL) << 16) | ((b3 & 0xFFL) << 24) |
                ((b4 & 0xFFL) << 32) | ((b5 & 0xFFL) << 40) | ((b6 & 0xFFL) << 48) | ((b7 & 0xFFL) << 56);
        } else {
            bits = (b7 & 0xFFL) | ((b6 & 0xFFL) << 8) | ((b5 & 0xFFL) << 16) | ((b4 & 0xFFL) << 24) |
                ((b3 & 0xFFL) << 32) | ((b2 & 0xFFL) << 40) | ((b1 & 0xFFL) << 48) | ((b0 & 0xFFL) << 56);
        }
        return bits;
    }

    boolean isEmpty() {
        return current >= buf.length;
    }

    byte get() {
        return buf[current++];
    }

    char getChar() {
        int bits = read16(buf, current);
        current += 2;
        return (char) bits;
    }

    short getShort() {
        int bits = read16(buf, current);
        current += 2;
        return (short) bits;
    }

    int getInt() {
        int bits = read32(buf, current);
        current += 4;
        return bits;
    }

    float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    long getLong() {
        long bits = read64(buf, current);
        current += 8;
        return bits;
    }

    double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

    void get(byte[] toBuf) {
        for (int i = 0; i < toBuf.length; i++) {
            toBuf[i] = buf[current + i];
        }
        current += toBuf.length;
    }
}

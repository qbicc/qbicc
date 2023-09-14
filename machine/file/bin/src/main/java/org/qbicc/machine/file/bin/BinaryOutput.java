package org.qbicc.machine.file.bin;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

import io.smallrye.common.function.ExceptionConsumer;

/**
 * A binary output stream that is more powerful than a regular output stream.
 */
public abstract class BinaryOutput implements Closeable {
    private final OutputStream asOutputStream = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
            i8(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            i8array(b, off, len);
        }

        @Override
        public void close() throws IOException {
            BinaryOutput.this.close();
        }
    };

    BinaryOutput() {
    }

    public static BinaryOutput create(Path path, ByteOrder order) throws IOException {
        return new FileChannelBinaryOutput(path, order);
    }

    /**
     * Create a binary output stream to a temporary data repository.
     * On close, the given action is performed, and then the temporary data is discarded.
     * The data may be stored in memory or on disk, and its location may change as the size of the data changes.
     *
     * @param order the byte order (must not be {@code null})
     * @param onClose the action to take on close (must not be {@code null})
     * @return the binary output for the temporary data (not {@code null})
     * @throws IOException if an error occurs
     */
    public static BinaryOutput temporary(ByteOrder order, ExceptionConsumer<BinaryInput, IOException> onClose) throws IOException {
        return new TemporaryBinaryOutput(order, onClose);
    }

    public abstract ByteOrder order();

    /**
     * Change the byte order.
     *
     * @param newOrder the new byte order (must not be {@code null})
     * @return the previous byte order (not {@code null})
     */
    public abstract ByteOrder order(ByteOrder newOrder);

    public OutputStream asOutputStream() {
        return asOutputStream;
    }

    public abstract void i8(int value) throws IOException;

    public void i16le(int value) throws IOException {
        i8(value);
        i8(value >> 8);
    }

    public void i16be(int value) throws IOException {
        i8(value >> 8);
        i8(value);
    }

    public void i16(int value) throws IOException {
        if (order() == ByteOrder.LITTLE_ENDIAN) {
            i16le(value);
        } else {
            i16be(value);
        }
    }

    public void i32le(int value) throws IOException {
        i16le(value);
        i16le(value >> 16);
    }

    public void i32be(int value) throws IOException {
        i16be(value >> 16);
        i16be(value);
    }

    public void i32(int value) throws IOException {
        if (order() == ByteOrder.LITTLE_ENDIAN) {
            i32le(value);
        } else {
            i32be(value);
        }
    }

    public void i64le(long value) throws IOException {
        i32le((int) value);
        i32le((int) (value >> 32));
    }

    public void i64be(long value) throws IOException {
        i32be((int) (value >> 32));
        i32be((int) value);
    }

    public void i64(long value) throws IOException {
        if (order() == ByteOrder.LITTLE_ENDIAN) {
            i64le(value);
        } else {
            i64be(value);
        }
    }

    public void i8array(byte[] array, int off, int len) throws IOException {
        for (int i = 0; i < len; i ++) {
            i8(array[i]);
        }
    }

    public void i8array(byte[] array) throws IOException {
        i8array(array, 0, array.length);
    }

    public void i8buffer(final ByteBuffer buffer) throws IOException {
        int rem = buffer.remaining();
        for (int i = 0; i < rem; i ++) {
            i8(buffer.get());
        }
    }

    public void i16array(short[] array, int off, int len) throws IOException {
        for (int i = 0; i < len; i ++) {
            i16(array[i]);
        }
    }

    public void i16array(short[] array) throws IOException {
        i16array(array, 0, array.length);
    }

    public void i32array(int[] array, int off, int len) throws IOException {
        for (int i = 0; i < len; i ++) {
            i32(array[i]);
        }
    }

    public void i32array(int[] array) throws IOException {
        i32array(array, 0, array.length);
    }

    public void i64array(long[] array, int off, int len) throws IOException {
        for (int i = 0; i < len; i ++) {
            i64(array[i]);
        }
    }

    public void i64array(long[] array) throws IOException {
        i64array(array, 0, array.length);
    }

    public void uleb(int val) throws IOException {
        int b;
        b = val & 0x7f;
        val >>>= 7;
        while (val != 0) {
            b |= 0x80;
            i8(b);
            b = val & 0x7f;
            val >>>= 7;
        }
        i8(b);
    }

    public void uleb(long val) throws IOException {
        int b;
        b = (int) (val & 0x7f);
        val >>>= 7;
        while (val != 0) {
            b |= 0x80;
            i8(b);
            b = (int) (val & 0x7f);
            val >>>= 7;
        }
        i8(b);
    }

    public void sleb(int val) throws IOException {
        int b;
        if (val < 0) {
            b = val & 0x7f;
            val >>= 7;
            while (val != -1) {
                b |= 0x80;
                i8(b);
                b = val & 0x7f;
                val >>= 7;
            }
            i8(b);
        } else {
            b = val & 0x7f;
            val >>>= 7;
            // write an extra byte if the sign bit is set on the last one
            while (val != 0 || b >= 0x40) {
                b |= 0x80;
                i8(b);
                b = val & 0x7f;
                val >>>= 7;
            }
            i8(b);
        }
    }

    public void sleb(long val) throws IOException {
        int b;
        if (val < 0) {
            b = (int) (val & 0x7f);
            val >>= 7;
            while (val != -1) {
                b |= 0x80;
                i8(b);
                b = (int) (val & 0x7f);
                val >>= 7;
            }
            i8(b);
        } else {
            b = (int) (val & 0x7f);
            val >>>= 7;
            // write an extra byte if the sign bit is set on the last one
            while (val != 0 || b >= 0x40) {
                b |= 0x80;
                i8(b);
                b = (int) (val & 0x7f);
                val >>>= 7;
            }
            i8(b);
        }
    }

    public void uleb(I128 val) throws IOException {
        uleb(val.low(), val.high());
    }

    public void uleb(long lo, long hi) throws IOException {
        int b;
        b = (int) (lo & 0x7f);
        lo = (lo >>> 7) | (hi << 57);
        hi >>>= 7;
        while (lo != 0 && hi != 0) {
            b |= 0x80;
            i8(b);
            b = (int) (lo & 0x7f);
            lo = (lo >>> 7) | (hi << 57);
            hi >>>= 7;
        }
        i8(b);
    }

    public void sleb(I128 val) throws IOException {
        sleb(val.low(), val.high());
    }

    public void sleb(long lo, long hi) throws IOException {
        int b;
        if (hi < 0) {
            b = (int) (hi & 0x7f);
            hi >>= 7;
            while (hi != -1 || lo != -1) {
                b |= 0x80;
                i8(b);
                b = (int) (lo & 0x7f);
                lo = (lo >>> 7) | (hi << 57);
                hi >>= 7;
            }
            i8(b);
        } else {
            b = (int) (hi & 0x7f);
            hi >>>= 7;
            // write an extra byte if the sign bit is set on the last one
            while (hi != 0 || lo != 0 || b >= 0x40) {
                b |= 0x80;
                i8(b);
                b = (int) (lo & 0x7f);
                lo = (lo >>> 7) | (hi << 57);
                hi >>>= 7;
            }
            i8(b);
        }
    }

    public void utf8(int codePoint, int minEnc) throws IOException {
        if (codePoint < 0 || minEnc < 1) {
            throw new IllegalArgumentException("Invalid code point or minimum encoding");
        } else if (codePoint < 0x80 && minEnc == 1) {
            i8(codePoint);
        } else if (codePoint < 0x800 && minEnc <= 2) {
            i8(0b110_00000 | (codePoint >> 6) & 0b000_11111);
            i8(0b10_000000 | codePoint & 0b00_111111);
        } else if (codePoint < 0x1_0000 && minEnc <= 3) {
            i8(0b1110_0000 | (codePoint >> 12) & 0b0000_1111);
            i8(0b10_000000 | (codePoint >> 6) & 0b00_111111);
            i8(0b10_000000 | codePoint & 0b00_111111);
        } else if (codePoint < 0x11_0000 && minEnc <= 4) {
            i8(0b11110_000 | (codePoint >> 18) & 0b00000_111);
            i8(0b10_000000 | (codePoint >> 12) & 0b00_111111);
            i8(0b10_000000 | (codePoint >> 6) & 0b00_111111);
            i8(0b10_000000 | codePoint & 0b00_111111);
        } else {
            throw new IllegalArgumentException("Invalid code point or minimum encoding");
        }
    }

    public void utf8(int codePoint) throws IOException {
        utf8(codePoint, 1);
    }

    public void utf8(String string) throws IOException {
        int cp;
        for (int i = 0; i < string.length(); i += Character.charCount(cp)) {
            cp = string.codePointAt(i);
            utf8(cp);
        }
    }

    public void utf8z(String string) throws IOException {
        int cp;
        for (int i = 0; i < string.length(); i += Character.charCount(cp)) {
            cp = string.codePointAt(i);
            if (cp == 0) {
                // prevent early termination by encoding 0 sneakily
                utf8(0, 2);
            } else {
                utf8(cp);
            }
        }
        i8(0);
    }

    public void transferFrom(InputStream is) throws IOException {
        is.transferTo(asOutputStream);
    }

    public abstract void close() throws IOException;
}

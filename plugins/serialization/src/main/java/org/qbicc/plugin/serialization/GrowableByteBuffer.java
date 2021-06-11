package org.qbicc.plugin.serialization;

import org.qbicc.context.CompilationContext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.IdentityHashMap;

/**
 * A utility class that provides an array-backed automatically resized ByteBuffer
 */
class GrowableByteBuffer {
    private static final int CHUNK_SIZE = 16; // 128 * 1024;  TODO: Set very small to test growing logic...don't leave it like this for long!

    private final ByteOrder endianness;
    private byte[] buffer;
    private ByteBuffer out;

    GrowableByteBuffer(ByteOrder endianness) {
        this.endianness = endianness;
        buffer = new byte[CHUNK_SIZE];
        out = ByteBuffer.wrap(buffer).order(endianness);
    }

    private void ensureCapacity(int desired) {
        if (out.position() + desired >= out.limit()) {
            int increment = desired < CHUNK_SIZE / 2 ? CHUNK_SIZE : desired * 2;
            buffer = Arrays.copyOf(buffer, buffer.length + increment);
            out = ByteBuffer.wrap(buffer).position(out.position()).order(endianness);
        }
    }

    void align(int alignment) {
        int cur = out.position();
        int aligned = (cur + alignment-1) & alignment;
        if (aligned != cur) {
            ensureCapacity(alignment);
            out.position(aligned);
        }
    }

    byte[] getBytes() {
        return Arrays.copyOf(buffer, out.position());
    }

    void putBoolean(boolean v) {
        ensureCapacity(1);
        out.put(v ? (byte)1 : (byte)0);
    }

    void putByte(byte v) {
        ensureCapacity(1);
        out.put(v);
    }

    void putShort(short v) {
        ensureCapacity(2);
        out.putShort(v);
    }

    void putChar(char v) {
        ensureCapacity(2);
        out.putChar(v);
    }

    void putInt(int v) {
        ensureCapacity(4);
        out.putInt(v);
    }

    void putFloat(float v) {
        ensureCapacity(4);
        out.putFloat(v);
    }

    void putLong(long v) {
        ensureCapacity(8);
        out.putLong(v);
    }

    void putDouble(double v) {
        ensureCapacity(8);
        out.putDouble(v);
    }

    void put(byte[] bytes) {
        ensureCapacity(bytes.length);
        out.put(bytes);
    }
}

package org.qbicc.plugin.serialization;

import org.qbicc.context.CompilationContext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.IdentityHashMap;

class HeapOutputStream {
    private static final int CHUNK_SIZE = 16; // 128 * 1024;  TODO: Set very small to test growing logic...don't leave it like this for long!

    private final ByteOrder endianness;
    private final IdentityHashMap<Object, Integer> objects = new IdentityHashMap<>();
    private int lastIndex = 0;
    private byte[] buffer;
    private ByteBuffer out;

    HeapOutputStream(ByteOrder endianness) {
        this.endianness = endianness;
        buffer = new byte[CHUNK_SIZE];
        out = ByteBuffer.wrap(buffer).order(endianness);
    }

    private void ensureCapacity(int desired) {
        if (out.position() + desired >= out.limit()) {
            buffer = Arrays.copyOf(buffer, buffer.length + CHUNK_SIZE);
            out = ByteBuffer.wrap(buffer).position(out.position()).order(endianness);
        }
    }

    int getBackref(Object obj) {
        if (objects.containsKey(obj)) {
            Integer prevIdx = objects.get(obj);
            return lastIndex - prevIdx;
        } else {
            lastIndex += 1;
            objects.put(obj, lastIndex);
            return -1;
        }
    }

    byte[] getBytes() {
        return Arrays.copyOf(buffer, out.position());
    }

    int getNumberOfObjects() {
        return lastIndex;
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

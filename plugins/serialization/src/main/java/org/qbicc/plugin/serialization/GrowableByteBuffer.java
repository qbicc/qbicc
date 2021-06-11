package org.qbicc.plugin.serialization;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.CompoundType;

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
    private final int pointerSize;
    private byte[] buffer;
    private ByteBuffer out;
    private int allocPtr;

    GrowableByteBuffer(ByteOrder endianness, int pointerSize) {
        this.endianness = endianness;
        this.pointerSize = pointerSize;
        buffer = new byte[CHUNK_SIZE];
        out = ByteBuffer.wrap(buffer).order(endianness);
        allocPtr = 0;
    }

    /**
     * Return an offset into the ByteBuffer that has been "allocated"
     * with the proper alignment and size to accommodate an instance
     * of the argument CompoundType.
     */
    int allocate(CompoundType ct, int trailingBytes) {
        int cur = allocPtr;
        int aligned = (cur + ct.getAlign()-1) & ~(ct.getAlign()-1);
        allocPtr = aligned + (int)ct.getSize() + trailingBytes;
        if (allocPtr >= buffer.length) {
            int newSize = buffer.length + CHUNK_SIZE;
            while (newSize < allocPtr) {
                newSize += CHUNK_SIZE;
            }
            buffer = Arrays.copyOf(buffer, newSize);
            out = ByteBuffer.wrap(buffer).order(endianness);
        }
        return aligned;
    }




    byte[] getBytes() {
        return Arrays.copyOf(buffer, allocPtr);
    }

    void putBoolean(int pos, boolean v) {
        out.put(pos, v ? (byte)1 : (byte)0);
    }

    void putByte(int pos, byte v) {
        out.put(pos, v);
    }

    void putShort(int pos, short v) {
        out.putShort(pos, v);
    }

    void putChar(int pos, char v) {
        out.putChar(pos, v);
    }

    void putInt(int pos, int v) {
        out.putInt(pos, v);
    }

    void putFloat(int pos, float v) {
        out.putFloat(pos, v);
    }

    void putLong(int pos, long v) {
        out.putLong(pos, v);
    }

    void putDouble(int pos, double v) {
        out.putDouble(pos, v);
    }

    void putPointer(int pos, int ptr) {
        if (pointerSize == 4) {
            this.putInt(pos, ptr);
        } else {
            this.putLong(pos, ptr);
        }
    }
}

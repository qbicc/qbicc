package org.qbicc.interpreter.memory;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.interpreter.Memory;

/**
 * A memory region which is backed by a {@code short} array which can be directly accessed.
 */
public final class ShortArrayMemory extends AbstractMemory {
    private static final VarHandle h16 = ConstantBootstraps.arrayVarHandle(MethodHandles.lookup(), "ignored", VarHandle.class, short[].class);

    private final short[] array;

    ShortArrayMemory(short[] array) {
        this.array = array;
    }

    public short[] getArray() {
        return array;
    }

    @Override
    public int load16(long index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return array[Math.toIntExact(index >>> 1)] & 0xffff;
        } else if (SingleOpaque.includes(mode)) {
            return (int) h16.getOpaque(array, Math.toIntExact(index >>> 1)) & 0xffff;
        } else if (GlobalAcquire.includes(mode)) {
            return (int) h16.getAcquire(array, Math.toIntExact(index >>> 1)) & 0xffff;
        } else {
            return (int) h16.getVolatile(array, Math.toIntExact(index >>> 1)) & 0xffff;
        }
    }

    @Override
    public void store16(long index, int value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            array[Math.toIntExact(index >>> 1)] = (short) value;
        } else if (SingleOpaque.includes(mode)) {
            h16.setOpaque(array, Math.toIntExact(index >>> 1), (short) value);
        } else if (GlobalRelease.includes(mode)) {
            h16.setRelease(array, Math.toIntExact(index >>> 1), (short) value);
        } else {
            h16.setVolatile(array, Math.toIntExact(index >>> 1), (short) value);
        }
    }

    @Override
    public void storeMemory(long destIndex, Memory src, long srcIndex, long size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeMemory(long destIndex, byte[] src, int srcIndex, int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareAndExchange16(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = array[Math.toIntExact(index >>> 1)] & 0xffff;
            if (val == (expect & 0xffff)) {
                array[Math.toIntExact(index >>> 1)] = (short) update;
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h16.compareAndExchangeAcquire(array, Math.toIntExact(index >>> 1), (short) expect, (short) update) & 0xffff;
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16.compareAndExchangeRelease(array, Math.toIntExact(index >>> 1), (short) expect, (short) update) & 0xffff;
        } else {
            return (int) h16.compareAndExchange(array, Math.toIntExact(index >>> 1), (short) expect, (short) update) & 0xffff;
        }
    }

    @Override
    public Memory copy(long newSize) {
        return new ShortArrayMemory(Arrays.copyOf(array, Math.toIntExact(newSize >>> 1)));
    }

    @Override
    public Memory clone() {
        return new ShortArrayMemory(array.clone());
    }

    @Override
    public Memory cloneZeroed() {
        return new ShortArrayMemory(new short[array.length]);
    }

    @Override
    public long getSize() {
        return (long) array.length << 1;
    }
}

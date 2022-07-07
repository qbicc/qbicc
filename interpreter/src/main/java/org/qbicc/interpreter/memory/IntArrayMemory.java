package org.qbicc.interpreter.memory;

import static org.qbicc.graph.atomic.AccessModes.GlobalAcquire;
import static org.qbicc.graph.atomic.AccessModes.GlobalPlain;
import static org.qbicc.graph.atomic.AccessModes.GlobalRelease;
import static org.qbicc.graph.atomic.AccessModes.SingleOpaque;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.interpreter.Memory;

/**
 * A memory region which is backed by an {@code int} array which can be directly accessed.
 */
public final class IntArrayMemory extends AbstractMemory {
    private static final VarHandle h32 = ConstantBootstraps.arrayVarHandle(MethodHandles.lookup(), "ignored", VarHandle.class, int[].class);

    private final int[] array;

    IntArrayMemory(int[] array) {
        this.array = array;
    }

    public int[] getArray() {
        return array;
    }

    @Override
    public int load32(long index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return array[Math.toIntExact(index >>> 2)];
        } else if (SingleOpaque.includes(mode)) {
            return (int) h32.getOpaque(array, Math.toIntExact(index >>> 2));
        } else if (GlobalAcquire.includes(mode)) {
            return (int) h32.getAcquire(array, Math.toIntExact(index >>> 2));
        } else {
            return (int) h32.getVolatile(array, Math.toIntExact(index >>> 2));
        }
    }

    @Override
    public void store32(long index, int value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            array[Math.toIntExact(index >>> 2)] = value;
        } else if (SingleOpaque.includes(mode)) {
            h32.setOpaque(array, Math.toIntExact(index >>> 2), value);
        } else if (GlobalRelease.includes(mode)) {
            h32.setRelease(array, Math.toIntExact(index >>> 2), value);
        } else {
            h32.setVolatile(array, Math.toIntExact(index >>> 2), value);
        }
    }

    @Override
    public int compareAndExchange32(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = array[Math.toIntExact(index >>> 2)];
            if (val == expect) {
                array[Math.toIntExact(index >>> 2)] = update;
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h32.compareAndExchangeAcquire(array, Math.toIntExact(index >>> 2), expect, update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32.compareAndExchangeRelease(array, Math.toIntExact(index >>> 2), expect, update);
        } else {
            return (int) h32.compareAndExchange(array, Math.toIntExact(index >>> 2), expect, update);
        }
    }

    @Override
    public Memory copy(long newSize) {
        return new IntArrayMemory(Arrays.copyOf(array, Math.toIntExact(newSize >>> 2)));
    }

    @Override
    public Memory clone() {
        return new IntArrayMemory(array.clone());
    }

    @Override
    public Memory cloneZeroed() {
        return new IntArrayMemory(new int[array.length]);
    }

    @Override
    public long getSize() {
        return (long) array.length << 2;
    }
}

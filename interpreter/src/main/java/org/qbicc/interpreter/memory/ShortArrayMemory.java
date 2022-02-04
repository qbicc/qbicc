package org.qbicc.interpreter.memory;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.impl.InvalidMemoryAccessException;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.ValueType;

/**
 * A memory region which is backed by a {@code short} array which can be directly accessed.
 */
public final class ShortArrayMemory implements Memory {
    private static final VarHandle h16 = ConstantBootstraps.arrayVarHandle(MethodHandles.lookup(), "ignored", VarHandle.class, short[].class);

    private final short[] array;

    ShortArrayMemory(short[] array) {
        this.array = array;
    }

    public short[] getArray() {
        return array;
    }

    @Override
    public int load8(long index, ReadAccessMode mode) {
        throw new InvalidMemoryAccessException();
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
    public int load32(long index, ReadAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public long load64(long index, ReadAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public VmObject loadRef(long index, ReadAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public ValueType loadType(long index, ReadAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public Pointer loadPointer(long index, ReadAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public void store8(long index, int value, WriteAccessMode mode) {
        throw new InvalidMemoryAccessException();
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
    public void store32(long index, int value, WriteAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public void store64(long index, long value, WriteAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public void storeRef(long index, VmObject value, WriteAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public void storeType(long index, ValueType value, WriteAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public void storePointer(long index, Pointer value, WriteAccessMode mode) {
        throw new InvalidMemoryAccessException();
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
    public int compareAndExchange8(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        throw new InvalidMemoryAccessException();
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
    public int compareAndExchange32(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public long compareAndExchange64(long index, long expect, long update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public VmObject compareAndExchangeRef(long index, VmObject expect, VmObject update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public ValueType compareAndExchangeType(long index, ValueType expect, ValueType update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public Pointer compareAndExchangePointer(long index, Pointer expect, Pointer update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        throw new InvalidMemoryAccessException();
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

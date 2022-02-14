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
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.impl.InvalidMemoryAccessException;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;

/**
 * A memory region which is backed by a reference array which can be directly accessed.
 */
public final class ReferenceArrayMemory implements Memory {
    private static final VarHandle hr = ConstantBootstraps.arrayVarHandle(MethodHandles.lookup(), "ignored", VarHandle.class, VmObject[].class);

    private final VmObject[] array;
    private final int shift;

    ReferenceArrayMemory(VmObject[] array, ReferenceType refType) {
        this(array, Long.numberOfTrailingZeros(refType.getSize()));
    }

    private ReferenceArrayMemory(VmObject[] array, int shift) {
        this.array = array;
        this.shift = shift;
    }

    public VmObject[] getArray() {
        return array;
    }

    @Override
    public int load8(long index, ReadAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public int load16(long index, ReadAccessMode mode) {
        throw new InvalidMemoryAccessException();
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
        if (GlobalPlain.includes(mode)) {
            return array[Math.toIntExact(index >>> shift)];
        } else if (SingleOpaque.includes(mode)) {
            return (VmObject) hr.getOpaque(array, Math.toIntExact(index >>> shift));
        } else if (GlobalAcquire.includes(mode)) {
            return (VmObject) hr.getAcquire(array, Math.toIntExact(index >>> shift));
        } else {
            return (VmObject) hr.getVolatile(array, Math.toIntExact(index >>> shift));
        }
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
        throw new InvalidMemoryAccessException();
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
        if (GlobalPlain.includes(mode)) {
            array[Math.toIntExact(index >>> shift)] = value;
        } else if (SingleOpaque.includes(mode)) {
            hr.setOpaque(array, Math.toIntExact(index >>> shift), value);
        } else if (GlobalRelease.includes(mode)) {
            hr.setRelease(array, Math.toIntExact(index >>> shift), value);
        } else {
            hr.setVolatile(array, Math.toIntExact(index >>> shift), value);
        }
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
        throw new InvalidMemoryAccessException();
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
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            VmObject val = array[Math.toIntExact(index >>> shift)];
            if (val == expect) {
                array[Math.toIntExact(index >>> shift)] = update;
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (VmObject) hr.compareAndExchangeAcquire(array, Math.toIntExact(index >>> shift), expect, update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (VmObject) hr.compareAndExchangeRelease(array, Math.toIntExact(index >>> shift), expect, update);
        } else {
            return (VmObject) hr.compareAndExchange(array, Math.toIntExact(index >>> shift), expect, update);
        }
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
        return new ReferenceArrayMemory(Arrays.copyOf(array, Math.toIntExact(newSize >>> shift)), shift);
    }

    @Override
    public Memory clone() {
        return new ReferenceArrayMemory(array.clone(), shift);
    }

    @Override
    public Memory cloneZeroed() {
        return new ReferenceArrayMemory(new VmObject[array.length], shift);
    }

    @Override
    public long getSize() {
        return 0;
    }
}

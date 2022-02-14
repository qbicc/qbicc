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
import org.qbicc.type.ValueType;

/**
 * A memory region which is backed by an {@code float} array which can be directly accessed.
 */
public final class FloatArrayMemory implements Memory {
    private static final VarHandle h32 = ConstantBootstraps.arrayVarHandle(MethodHandles.lookup(), "ignored", VarHandle.class, float[].class);

    private final float[] array;

    FloatArrayMemory(float[] array) {
        this.array = array;
    }

    public float[] getArray() {
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
        if (GlobalPlain.includes(mode)) {
            return Float.floatToRawIntBits(array[Math.toIntExact(index >>> 2)]);
        } else if (SingleOpaque.includes(mode)) {
            return Float.floatToRawIntBits((float) h32.getOpaque(array, Math.toIntExact(index >>> 2)));
        } else if (GlobalAcquire.includes(mode)) {
            return Float.floatToRawIntBits((float) h32.getAcquire(array, Math.toIntExact(index >>> 2)));
        } else {
            return Float.floatToRawIntBits((float) h32.getVolatile(array, Math.toIntExact(index >>> 2)));
        }
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
        throw new InvalidMemoryAccessException();
    }

    @Override
    public void store32(long index, int value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            array[Math.toIntExact(index >>> 2)] = Float.intBitsToFloat(value);
        } else if (SingleOpaque.includes(mode)) {
            h32.setOpaque(array, Math.toIntExact(index >>> 2), Float.intBitsToFloat(value));
        } else if (GlobalRelease.includes(mode)) {
            h32.setRelease(array, Math.toIntExact(index >>> 2), Float.intBitsToFloat(value));
        } else {
            h32.setVolatile(array, Math.toIntExact(index >>> 2), Float.intBitsToFloat(value));
        }
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
        throw new InvalidMemoryAccessException();
    }

    @Override
    public int compareAndExchange32(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            float val = array[Math.toIntExact(index >>> 2)];
            if (val == expect) {
                array[Math.toIntExact(index >>> 2)] = Float.intBitsToFloat(update);
            }
            return Float.floatToRawIntBits(val);
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return Float.floatToRawIntBits((float) h32.compareAndExchangeAcquire(array, Math.toIntExact(index >>> 2), Float.intBitsToFloat(expect), Float.intBitsToFloat(update)));
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return Float.floatToRawIntBits((float) h32.compareAndExchangeRelease(array, Math.toIntExact(index >>> 2), Float.intBitsToFloat(expect), Float.intBitsToFloat(update)));
        } else {
            return Float.floatToRawIntBits((float) h32.compareAndExchange(array, Math.toIntExact(index >>> 2), Float.intBitsToFloat(expect), Float.intBitsToFloat(update)));
        }
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
        return new FloatArrayMemory(Arrays.copyOf(array, Math.toIntExact(newSize >>> 2)));
    }

    @Override
    public Memory clone() {
        return new FloatArrayMemory(array.clone());
    }

    @Override
    public Memory cloneZeroed() {
        return new FloatArrayMemory(new float[array.length]);
    }

    @Override
    public long getSize() {
        return (long) array.length << 2;
    }
}

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
 * A memory region which is backed by a {@code double} array which can be directly accessed.
 */
public final class DoubleArrayMemory extends AbstractMemory {
    private static final VarHandle h64 = ConstantBootstraps.arrayVarHandle(MethodHandles.lookup(), "ignored", VarHandle.class, double[].class);

    private final double[] array;

    DoubleArrayMemory(double[] array) {
        this.array = array;
    }

    public double[] getArray() {
        return array;
    }

    @Override
    public long load64(long index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return Double.doubleToRawLongBits(array[Math.toIntExact(index >>> 3)]);
        } else if (SingleOpaque.includes(mode)) {
            return Double.doubleToRawLongBits((double) h64.getOpaque(array, Math.toIntExact(index >>> 3)));
        } else if (GlobalAcquire.includes(mode)) {
            return Double.doubleToRawLongBits((double) h64.getAcquire(array, Math.toIntExact(index >>> 3)));
        } else {
            return Double.doubleToRawLongBits((double) h64.getVolatile(array, Math.toIntExact(index >>> 3)));
        }
    }

    @Override
    public void store64(long index, long value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            array[Math.toIntExact(index >>> 3)] = Double.longBitsToDouble(value);
        } else if (SingleOpaque.includes(mode)) {
            h64.setOpaque(array, Math.toIntExact(index >>> 3), Double.longBitsToDouble(value));
        } else if (GlobalRelease.includes(mode)) {
            h64.setRelease(array, Math.toIntExact(index >>> 3), Double.longBitsToDouble(value));
        } else {
            h64.setVolatile(array, Math.toIntExact(index >>> 3), Double.longBitsToDouble(value));
        }
    }

    @Override
    public long compareAndExchange64(long index, long expect, long update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            double val = array[Math.toIntExact(index >>> 3)];
            if (val == expect) {
                array[Math.toIntExact(index >>> 3)] = Double.longBitsToDouble(update);
            }
            return Double.doubleToRawLongBits(val);
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return Double.doubleToRawLongBits((double) h64.compareAndExchangeAcquire(array, Math.toIntExact(index >>> 3), Double.longBitsToDouble(expect), Double.longBitsToDouble(update)));
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return Double.doubleToRawLongBits((double) h64.compareAndExchangeRelease(array, Math.toIntExact(index >>> 3), Double.longBitsToDouble(expect), Double.longBitsToDouble(update)));
        } else {
            return Double.doubleToRawLongBits((double) h64.compareAndExchange(array, Math.toIntExact(index >>> 3), Double.longBitsToDouble(expect), Double.longBitsToDouble(update)));
        }
    }

    @Override
    public Memory copy(long newSize) {
        return new DoubleArrayMemory(Arrays.copyOf(array, Math.toIntExact(newSize >>> 3)));
    }

    @Override
    public Memory clone() {
        return new DoubleArrayMemory(array.clone());
    }

    @Override
    public Memory cloneZeroed() {
        return new DoubleArrayMemory(new double[array.length]);
    }

    @Override
    public long getSize() {
        return (long) array.length << 3;
    }
}

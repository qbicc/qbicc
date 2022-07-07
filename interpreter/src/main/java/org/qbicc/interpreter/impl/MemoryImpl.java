package org.qbicc.interpreter.impl;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.ValueType;

/**
 *
 */
abstract class MemoryImpl implements Memory {
    private static final byte[] NO_DATA = new byte[0];
    private static final Object[] NO_THINGS = new Object[0];

    private static final VarHandle ht = MethodHandles.arrayElementVarHandle(Object[].class);
    private static final VarHandle h8 = MethodHandles.arrayElementVarHandle(byte[].class);

    // Data are items that can be represented directly as bytes and have a minimum alignment of 1
    final byte[] data;
    // Things are items that must be represented as references and have a minimum alignment of 2
    final Object[] things;

    MemoryImpl(int dataSize) {
        // round up to hold one whole Thing
        int thingSize = (dataSize + 1) >> 1;
        dataSize = thingSize << 1;
        data = dataSize == 0 ? NO_DATA : new byte[dataSize];
        things = dataSize == 0 ? NO_THINGS : new Object[thingSize];
    }

    MemoryImpl(MemoryImpl original) {
        data = original.data.clone();
        things = original.things.clone();
    }

    MemoryImpl(MemoryImpl original, int newSize) {
        int thingSize = (newSize + 1) >> 1;
        newSize = thingSize << 1;
        data = Arrays.copyOf(original.data, newSize);
        things = Arrays.copyOf(original.things, thingSize);
    }

    static void checkAlign(long offs, int align) {
        int mask = align - 1;
        if ((offs & mask) != 0) {
            throw new IllegalArgumentException("Invalid unaligned access: 0x" + Long.toHexString(offs));
        }
    }

    @Override
    public final int load8(long index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return (int) h8.get(data, Math.toIntExact(index));
        } else if (SingleOpaque.includes(mode)) {
            return (int) h8.getOpaque(data, Math.toIntExact(index));
        } else if (GlobalAcquire.includes(mode)) {
            return (int) h8.getAcquire(data, Math.toIntExact(index));
        } else {
            return (int) h8.getVolatile(data, Math.toIntExact(index));
        }
    }

    @Override
    public abstract int load16(long index, ReadAccessMode mode);

    @Override
    public abstract int load32(long index, ReadAccessMode mode);

    @Override
    public abstract long load64(long index, ReadAccessMode mode);

    @Override
    public VmObject loadRef(long index, ReadAccessMode mode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(mode)) {
            return (VmObject) ht.get(things, Math.toIntExact(index >> 1));
        } else if (SingleOpaque.includes(mode)) {
            return (VmObject) ht.getOpaque(things, Math.toIntExact(index >> 1));
        } else if (GlobalAcquire.includes(mode)) {
            return (VmObject) ht.getAcquire(things, Math.toIntExact(index >> 1));
        } else {
            return (VmObject) ht.getVolatile(things, Math.toIntExact(index >> 1));
        }
    }

    @Override
    public ValueType loadType(long index, ReadAccessMode mode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(mode)) {
            return (ValueType) ht.get(things, Math.toIntExact(index >> 1));
        } else if (SingleOpaque.includes(mode)) {
            return (ValueType) ht.getOpaque(things, Math.toIntExact(index >> 1));
        } else if (GlobalAcquire.includes(mode)) {
            return (ValueType) ht.getAcquire(things, Math.toIntExact(index >> 1));
        } else {
            return (ValueType) ht.getVolatile(things, Math.toIntExact(index >> 1));
        }
    }

    @Override
    public Pointer loadPointer(long index, ReadAccessMode mode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(mode)) {
            return (Pointer) ht.get(things, Math.toIntExact(index >> 1));
        } else if (SingleOpaque.includes(mode)) {
            return (Pointer) ht.getOpaque(things, Math.toIntExact(index >> 1));
        } else if (GlobalAcquire.includes(mode)) {
            return (Pointer) ht.getAcquire(things, Math.toIntExact(index >> 1));
        } else {
            return (Pointer) ht.getVolatile(things, Math.toIntExact(index >> 1));
        }
    }

    @Override
    public final void store8(long index, int value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            h8.set(data, Math.toIntExact(index), (byte) value);
        } else if (SingleOpaque.includes(mode)) {
            h8.setOpaque(data, Math.toIntExact(index), (byte) value);
        } else if (GlobalRelease.includes(mode)) {
            h8.setRelease(data, Math.toIntExact(index), (byte) value);
        } else {
            h8.setVolatile(data, Math.toIntExact(index), (byte) value);
        }
    }

    @Override
    public abstract void store16(long index, int value, WriteAccessMode mode);

    @Override
    public abstract void store32(long index, int value, WriteAccessMode mode);

    @Override
    public abstract void store64(long index, long value, WriteAccessMode mode);

    @Override
    public void storeRef(long index, VmObject value, WriteAccessMode mode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(mode)) {
            ht.set(things, Math.toIntExact(index >> 1), value);
        } else if (SingleOpaque.includes(mode)) {
            ht.setOpaque(things, Math.toIntExact(index >> 1), value);
        } else if (GlobalRelease.includes(mode)) {
            ht.setRelease(things, Math.toIntExact(index >> 1), value);
        } else {
            ht.setVolatile(things, Math.toIntExact(index >> 1), value);
        }
    }

    @Override
    public void storeType(long index, ValueType value, WriteAccessMode mode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(mode)) {
            ht.set(things, Math.toIntExact(index >> 1), value);
        } else if (SingleOpaque.includes(mode)) {
            ht.setOpaque(things, Math.toIntExact(index >> 1), value);
        } else if (GlobalRelease.includes(mode)) {
            ht.setRelease(things, Math.toIntExact(index >> 1), value);
        } else {
            ht.setVolatile(things, Math.toIntExact(index >> 1), value);
        }
    }

    @Override
    public void storePointer(long index, Pointer value, WriteAccessMode mode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(mode)) {
            ht.set(things, Math.toIntExact(index >> 1), value);
        } else if (SingleOpaque.includes(mode)) {
            ht.setOpaque(things, Math.toIntExact(index >> 1), value);
        } else if (GlobalRelease.includes(mode)) {
            ht.setRelease(things, Math.toIntExact(index >> 1), value);
        } else {
            ht.setVolatile(things, Math.toIntExact(index >> 1), value);
        }
    }

    @Override
    public final int compareAndExchange8(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load8(index, readMode) & 0xff;
            if (val == (expect & 0xff)) {
                store8(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h8.compareAndExchangeAcquire(data, Math.toIntExact(index), (byte) expect, (byte) update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h8.compareAndExchangeRelease(data, Math.toIntExact(index), (byte) expect, (byte) update);
        } else {
            return (int) h8.compareAndExchange(data, Math.toIntExact(index), (byte) expect, (byte) update);
        }
    }

    @Override
    public abstract int compareAndExchange16(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public abstract int compareAndExchange32(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public abstract long compareAndExchange64(long index, long expect, long update, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public VmObject compareAndExchangeRef(long index, VmObject expect, VmObject update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            VmObject val = loadRef(index, readMode);
            if (val == expect) {
                storeRef(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (VmObject) ht.compareAndExchangeAcquire(things, Math.toIntExact(index >> 1), expect, update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (VmObject) ht.compareAndExchangeRelease(things, Math.toIntExact(index >> 1), expect, update);
        } else {
            return (VmObject) ht.compareAndExchange(things, Math.toIntExact(index >> 1), expect, update);
        }
    }

    @Override
    public ValueType compareAndExchangeType(long index, ValueType expect, ValueType update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            ValueType val = loadType(index, readMode);
            if (val == expect) {
                storeType(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (ValueType) ht.compareAndExchangeAcquire(things, Math.toIntExact(index >> 1), expect, update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (ValueType) ht.compareAndExchangeRelease(things, Math.toIntExact(index >> 1), expect, update);
        } else {
            return (ValueType) ht.compareAndExchange(things, Math.toIntExact(index >> 1), expect, update);
        }
    }

    @Override
    public Pointer compareAndExchangePointer(long index, Pointer expect, Pointer update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            Pointer val = loadPointer(index, readMode);
            if (val == expect) {
                storePointer(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (Pointer) ht.compareAndExchangeAcquire(things, Math.toIntExact(index >> 1), expect, update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (Pointer) ht.compareAndExchangeRelease(things, Math.toIntExact(index >> 1), expect, update);
        } else {
            return (Pointer) ht.compareAndExchange(things, Math.toIntExact(index >> 1), expect, update);
        }
    }

    @Override
    public final int getAndSet8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load8(index, readMode);
            store8(index, value, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h8.getAndSetAcquire(data, Math.toIntExact(index), (byte) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h8.getAndSetRelease(data, Math.toIntExact(index), (byte) value);
        } else {
            return (int) h8.getAndSet(data, Math.toIntExact(index), (byte) value);
        }
    }

    @Override
    public abstract int getAndSet16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public abstract int getAndSet32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public abstract long getAndSet64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public VmObject getAndSetRef(long index, VmObject value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            VmObject val = loadRef(index, readMode);
            storeRef(index, value, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (VmObject) ht.getAndSetAcquire(things, Math.toIntExact(index >> 1), value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (VmObject) ht.getAndSetRelease(things, Math.toIntExact(index >> 1), value);
        } else {
            return (VmObject) ht.getAndSet(things, Math.toIntExact(index >> 1), value);
        }
    }

    @Override
    public ValueType getAndSetType(long index, ValueType value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            ValueType val = loadType(index, readMode);
            storeType(index, value, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (ValueType) ht.getAndSetAcquire(things, Math.toIntExact(index >> 1), value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (ValueType) ht.getAndSetRelease(things, Math.toIntExact(index >> 1), value);
        } else {
            return (ValueType) ht.getAndSet(things, Math.toIntExact(index >> 1), value);
        }
    }

    @Override
    public Pointer getAndSetPointer(long index, Pointer value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            Pointer val = loadPointer(index, readMode);
            storePointer(index, value, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (Pointer) ht.getAndSetAcquire(things, Math.toIntExact(index >> 1), value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (Pointer) ht.getAndSetRelease(things, Math.toIntExact(index >> 1), value);
        } else {
            return (Pointer) ht.getAndSet(things, Math.toIntExact(index >> 1), value);
        }
    }

    @Override
    public final int getAndAdd8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load8(index, readMode);
            store8(index, value + val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h8.getAndAddAcquire(data, Math.toIntExact(index), (byte) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h8.getAndAddRelease(data, Math.toIntExact(index), (byte) value);
        } else {
            return (int) h8.getAndAdd(data, Math.toIntExact(index), (byte) value);
        }
    }

    @Override
    public abstract int getAndAdd16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public abstract int getAndAdd32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public abstract long getAndAdd64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public int getAndBitwiseAnd8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load8(index, readMode);
            store8(index, value & val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h8.getAndBitwiseAndAcquire(data, Math.toIntExact(index), (byte) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h8.getAndBitwiseAndRelease(data, Math.toIntExact(index), (byte) value);
        } else {
            return (int) h8.getAndBitwiseAnd(data, Math.toIntExact(index), (byte) value);
        }
    }

    @Override
    public int getAndBitwiseOr8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load8(index, readMode);
            store8(index, value | val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h8.getAndBitwiseOrAcquire(data, Math.toIntExact(index), (byte) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h8.getAndBitwiseOrRelease(data, Math.toIntExact(index), (byte) value);
        } else {
            return (int) h8.getAndBitwiseOr(data, Math.toIntExact(index), (byte) value);
        }
    }

    @Override
    public int getAndBitwiseXor8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load8(index, readMode);
            store8(index, value ^ val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h8.getAndBitwiseXorAcquire(data, Math.toIntExact(index), (byte) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h8.getAndBitwiseXorRelease(data, Math.toIntExact(index), (byte) value);
        } else {
            return (int) h8.getAndBitwiseXor(data, Math.toIntExact(index), (byte) value);
        }
    }

    @Override
    public void storeMemory(long destIndex, Memory src, long srcIndex, long size) {
        if (size > 0) {
            MemoryImpl srcImpl = (MemoryImpl) src;
            System.arraycopy(srcImpl.data, Math.toIntExact(srcIndex), data, Math.toIntExact(destIndex), Math.toIntExact(size));
            // misaligned copies of things will get weird results
            System.arraycopy(srcImpl.things, Math.toIntExact((srcIndex + 1) >> 1), things, Math.toIntExact((destIndex + 1) >> 1), Math.toIntExact((size + 1) >> 1));
        }
    }

    @Override
    public void storeMemory(long destIndex, byte[] src, int srcIndex, int size) {
        if (size > 0) {
            // just data
            System.arraycopy(src, srcIndex, data, Math.toIntExact(destIndex), size);
            // clear corresponding things
            Arrays.fill(things, Math.toIntExact(destIndex >> 1), Math.toIntExact((destIndex + size + 1) >> 1), null);
        }
    }

    @Override
    public abstract MemoryImpl copy(long newSize);

    byte[] getArray() {
        return data;
    }

    @Override
    public abstract MemoryImpl clone();
}

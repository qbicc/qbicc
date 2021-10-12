package org.qbicc.interpreter.impl;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;
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

    static int alignGap(final int align, final int offset) {
        int mask = align - 1;
        return align - offset & mask;
    }

    static void checkAlign(int offs, int align) {
        int mask = align - 1;
        if ((offs & mask) != 0) {
            throw new IllegalArgumentException("Invalid unaligned access: 0x" + Integer.toHexString(offs));
        }
    }

    @Override
    public final int load8(int index, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            return (int) h8.get(data, index);
        } else if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h8.getAcquire(data, index);
        } else {
            return (int) h8.getVolatile(data, index);
        }
    }

    @Override
    public abstract int load16(int index, MemoryAtomicityMode mode);

    @Override
    public abstract int load32(int index, MemoryAtomicityMode mode);

    @Override
    public abstract long load64(int index, MemoryAtomicityMode mode);

    @Override
    public VmObject loadRef(int index, MemoryAtomicityMode mode) {
        checkAlign(index, 2);
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            return (VmObject) ht.get(things, index >> 1);
        } else if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (VmObject) ht.getAcquire(things, index >> 1);
        } else {
            return (VmObject) ht.getVolatile(things, index >> 1);
        }
    }

    @Override
    public ValueType loadType(int index, MemoryAtomicityMode mode) {
        checkAlign(index, 2);
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            return (ValueType) ht.get(things, index >> 1);
        } else if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (ValueType) ht.getAcquire(things, index >> 1);
        } else {
            return (ValueType) ht.getVolatile(things, index >> 1);
        }
    }

    @Override
    public final void store8(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            h8.set(data, index, (byte) value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            h8.setRelease(data, index, (byte) value);
        } else {
            h8.setVolatile(data, index, (byte) value);
        }
    }

    @Override
    public abstract void store16(int index, int value, MemoryAtomicityMode mode);

    @Override
    public abstract void store32(int index, int value, MemoryAtomicityMode mode);

    @Override
    public abstract void store64(int index, long value, MemoryAtomicityMode mode);

    @Override
    public void storeRef(int index, VmObject value, MemoryAtomicityMode mode) {
        checkAlign(index, 2);
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            ht.set(things, index >> 1, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            ht.setRelease(things, index >> 1, value);
        } else {
            ht.setVolatile(things, index >> 1, value);
        }
    }

    @Override
    public void storeType(int index, ValueType value, MemoryAtomicityMode mode) {
        checkAlign(index, 2);
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            ht.set(things, index >> 1, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            ht.setRelease(things, index >> 1, value);
        } else {
            ht.setVolatile(things, index >> 1, value);
        }
    }

    @Override
    public final int compareAndExchange8(int index, int expect, int update, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h8.compareAndExchangeAcquire(data, index, (byte) expect, (byte) update);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h8.compareAndExchangeRelease(data, index, (byte) expect, (byte) update);
        } else {
            return (int) h8.compareAndExchange(data, index, (byte) expect, (byte) update);
        }
    }

    @Override
    public abstract int compareAndExchange16(int index, int expect, int update, MemoryAtomicityMode mode);

    @Override
    public abstract int compareAndExchange32(int index, int expect, int update, MemoryAtomicityMode mode);

    @Override
    public abstract long compareAndExchange64(int index, long expect, long update, MemoryAtomicityMode mode);

    @Override
    public VmObject compareAndExchangeRef(int index, VmObject expect, VmObject update, MemoryAtomicityMode mode) {
        checkAlign(index, 2);
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (VmObject) ht.compareAndExchangeAcquire(things, index, expect, update);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (VmObject) ht.compareAndExchangeRelease(things, index, expect, update);
        } else {
            return (VmObject) ht.compareAndExchange(things, index, expect, update);
        }
    }

    @Override
    public ValueType compareAndExchangeType(int index, ValueType expect, ValueType update, MemoryAtomicityMode mode) {
        checkAlign(index, 2);
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (ValueType) ht.compareAndExchangeAcquire(things, index, expect, update);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (ValueType) ht.compareAndExchangeRelease(things, index, expect, update);
        } else {
            return (ValueType) ht.compareAndExchange(things, index, expect, update);
        }
    }

    @Override
    public final int getAndSet8(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h8.getAndSetAcquire(data, index, (byte) value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h8.getAndSetRelease(data, index, (byte) value);
        } else {
            return (int) h8.getAndSet(data, index, (byte) value);
        }
    }

    @Override
    public abstract int getAndSet16(int index, int value, MemoryAtomicityMode mode);

    @Override
    public abstract int getAndSet32(int index, int value, MemoryAtomicityMode mode);

    @Override
    public abstract long getAndSet64(int index, long value, MemoryAtomicityMode mode);

    @Override
    public VmObject getAndSetRef(int index, VmObject value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (VmObject) ht.getAndSetAcquire(things, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (VmObject) ht.getAndSetRelease(things, index, value);
        } else {
            return (VmObject) ht.getAndSet(things, index, value);
        }
    }

    @Override
    public ValueType getAndSetType(int index, ValueType value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (ValueType) ht.getAndSetAcquire(things, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (ValueType) ht.getAndSetRelease(things, index, value);
        } else {
            return (ValueType) ht.getAndSet(things, index, value);
        }
    }

    @Override
    public final int getAndAdd8(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h8.getAndAddAcquire(data, index, (byte) value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h8.getAndAddRelease(data, index, (byte) value);
        } else {
            return (int) h8.getAndAdd(data, index, (byte) value);
        }
    }

    @Override
    public abstract int getAndAdd16(int index, int value, MemoryAtomicityMode mode);

    @Override
    public abstract int getAndAdd32(int index, int value, MemoryAtomicityMode mode);

    @Override
    public abstract long getAndAdd64(int index, long value, MemoryAtomicityMode mode);

    @Override
    public void storeMemory(int destIndex, Memory src, int srcIndex, int size) {
        if (size > 0) {
            MemoryImpl srcImpl = (MemoryImpl) src;
            System.arraycopy(srcImpl.data, srcIndex, data, destIndex, size);
            // misaligned copies of things will get weird results
            System.arraycopy(srcImpl.things, (srcIndex + 1) >> 1, things, (destIndex + 1) >> 1, (size + 1) >> 1);
        }
    }

    @Override
    public void storeMemory(int destIndex, byte[] src, int srcIndex, int size) {
        if (size > 0) {
            // just data
            System.arraycopy(src, srcIndex, data, destIndex, size);
            // clear corresponding things
            Arrays.fill(things, destIndex >> 1, (destIndex + size + 1) >> 1, null);
        }
    }

    @Override
    public abstract MemoryImpl copy(int newSize);

    byte[] getArray() {
        return data;
    }
}

package org.qbicc.interpreter.impl;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.qbicc.graph.MemoryAtomicityMode;

final class LittleEndianMemoryImpl extends MemoryImpl {
    static final LittleEndianMemoryImpl EMPTY = new LittleEndianMemoryImpl(0);

    private static final VarHandle h16 = MethodHandles.byteArrayViewVarHandle(short[].class, LITTLE_ENDIAN);
    private static final VarHandle h32 = MethodHandles.byteArrayViewVarHandle(int[].class, LITTLE_ENDIAN);
    private static final VarHandle h64 = MethodHandles.byteArrayViewVarHandle(long[].class, LITTLE_ENDIAN);

    LittleEndianMemoryImpl(int dataSize) {
        super(dataSize);
    }

    @Override
    public int load16(int index, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            return (int) h16.get(data, index);
        } else if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h16.getAcquire(data, index);
        } else {
            return (int) h16.getVolatile(data, index);
        }
    }

    @Override
    public int load32(int index, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            return (int) h32.get(data, index);
        } else if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h32.getAcquire(data, index);
        } else {
            return (int) h32.getVolatile(data, index);
        }
    }

    @Override
    public long load64(int index, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            return (long) h64.get(data, index);
        } else if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (long) h64.getAcquire(data, index);
        } else {
            return (long) h64.getVolatile(data, index);
        }
    }

    @Override
    public void store16(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            h16.set(data, index, (short) value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            h16.setRelease(data, index, (short) value);
        } else {
            h16.setVolatile(data, index, (short) value);
        }
    }

    @Override
    public void store32(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            h32.set(data, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            h32.setRelease(data, index, value);
        } else {
            h32.setVolatile(data, index, value);
        }
    }

    @Override
    public void store64(int index, long value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            h64.set(data, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            h64.setRelease(data, index, value);
        } else {
            h64.setVolatile(data, index, value);
        }
    }

    @Override
    public int compareAndExchange16(int index, int expect, int update, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h16.compareAndExchangeAcquire(data, index, (short) expect, (short) update);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h16.compareAndExchangeRelease(data, index, (short) expect, (short) update);
        } else {
            return (int) h16.compareAndExchange(data, index, (short) expect, (short) update);
        }
    }

    @Override
    public int compareAndExchange32(int index, int expect, int update, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h32.compareAndExchangeAcquire(data, index, expect, update);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h32.compareAndExchangeRelease(data, index, expect, update);
        } else {
            return (int) h32.compareAndExchange(data, index, expect, update);
        }
    }

    @Override
    public long compareAndExchange64(int index, long expect, long update, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (long) h64.compareAndExchangeAcquire(data, index, expect, update);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (long) h64.compareAndExchangeRelease(data, index, expect, update);
        } else {
            return (long) h64.compareAndExchange(data, index, expect, update);
        }
    }

    @Override
    public int getAndSet16(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h16.getAndSetAcquire(data, index, (short) value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h16.getAndSetRelease(data, index, (short) value);
        } else {
            return (int) h16.getAndSet(data, index, (short) value);
        }
    }

    @Override
    public int getAndSet32(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h32.getAndSetAcquire(data, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h32.getAndSetRelease(data, index, value);
        } else {
            return (int) h32.getAndSet(data, index, value);
        }
    }

    @Override
    public long getAndSet64(int index, long value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (long) h64.getAndSetAcquire(data, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (long) h64.getAndSetRelease(data, index, value);
        } else {
            return (long) h64.getAndSet(data, index, value);
        }
    }

    @Override
    public int getAndAdd16(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h16.getAndAddAcquire(data, index, (short) value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h16.getAndAddRelease(data, index, (short) value);
        } else {
            return (int) h16.getAndAdd(data, index, (short) value);
        }
    }

    @Override
    public int getAndAdd32(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h32.getAndAddAcquire(data, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h32.getAndAddRelease(data, index, value);
        } else {
            return (int) h32.getAndAdd(data, index, value);
        }
    }

    @Override
    public long getAndAdd64(int index, long value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (long) h64.getAndAddAcquire(data, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (long) h64.getAndAddRelease(data, index, value);
        } else {
            return (long) h64.getAndAdd(data, index, value);
        }
    }

    @Override
    public LittleEndianMemoryImpl copy(int newSize) {
        if (newSize == 0) {
            return EMPTY;
        }
        LittleEndianMemoryImpl newMemory = new LittleEndianMemoryImpl(newSize);
        newMemory.storeMemory(0, this, 0, Math.min(data.length, newMemory.data.length));
        return newMemory;
    }

}
